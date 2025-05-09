package com.zazhi.geoflow.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.zazhi.geoflow.config.properties.MinioConfigProperties;
import com.zazhi.geoflow.entity.pojo.DataSet;
import com.zazhi.geoflow.entity.pojo.GeoFile;
import com.zazhi.geoflow.entity.pojo.PageResult;
import com.zazhi.geoflow.entity.vo.GeoFileMetadataVO;
import com.zazhi.geoflow.mapper.DataSetMapper;
import com.zazhi.geoflow.mapper.GeoFileMapper;
import com.zazhi.geoflow.service.GeoFileService;
import com.zazhi.geoflow.utils.GeoFileUtil;
import com.zazhi.geoflow.utils.ImageUtil;
import com.zazhi.geoflow.utils.MinioUtil;
import com.zazhi.geoflow.utils.ThreadLocalUtil;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.processing.CoverageProcessor;
import org.geotools.coverage.processing.EmptyIntersectionException;
import org.geotools.gce.geotiff.GeoTiffFormat;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.coverage.grid.GridEnvelope;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.io.*;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * @author zazhi
 * @date 2025/3/20
 * @description: 文件
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GeoFileServiceImpl implements GeoFileService {

    private final MinioUtil minioUtil;

    private final GeoFileMapper geoFileMapper;

    private final MinioClient minioClient;

    private final MinioConfigProperties minioProp;

    private final DataSetMapper dataSetMapper;

    private final ImageUtil imageUtil;

    private final GeoFileUtil geoFileUtil;

    /**
     * 上传文件
     *
     * @param file 文件
     * @return 文件路径
     */
    @Override
    public String upload(MultipartFile file, String objectName, String fileName, String description) {
        String originalFilename = file.getOriginalFilename();
        // 文件名后缀
        objectName += originalFilename.substring(originalFilename.lastIndexOf("."));

        String url = minioUtil.upload(file, objectName);

        GeoFile geoFile = GeoFile.builder()
                .userId(ThreadLocalUtil.getCurrentId())
                .fileName(fileName)
                .objectName(objectName)
                .url(url)
                .fileSize(file.getSize())
//                .fileType(url.substring(url.lastIndexOf(".")))
                .description(description)
                .build();

        geoFileMapper.insert(geoFile);
        return url;
    }

    /**
     * 删除文件
     *
     * @param id 文件id
     */
    @Override
    public void delete(Integer id) {
        GeoFile geoFile = geoFileUtil.checkFile(id);
        // 删除minio文件 & 删除数据库记录
        minioUtil.remove(geoFile.getObjectName());
        geoFileMapper.delete(id);
    }



    /**
     * 获取文件元数据
     *
     * @param id 文件id
     * @return 文件元数据
     */
    @Override
    public GeoFileMetadataVO getMetadata(Integer id) {
        GeoFile geoFile = geoFileUtil.checkFile(id);

        // 从minio读取文件
        GeoTiffReader reader = null;
        try (InputStream inputStream = minioUtil.getObject(minioProp.getBucketName(), geoFile.getObjectName())) {
            reader = new GeoTiffReader(inputStream);
        } catch (Exception e) {
            throw new RuntimeException("读取文件失败");
        }

        // 获取文件元数据
        GeoFileMetadataVO metadataVO = new GeoFileMetadataVO();
        // 1. 地理范围
        GeneralEnvelope envelope = reader.getOriginalEnvelope();
        metadataVO.setMinX(envelope.getMinimum(0));
        metadataVO.setMinY(envelope.getMinimum(1));
        metadataVO.setMaxX(envelope.getMaximum(0));
        metadataVO.setMaxY(envelope.getMaximum(1));

        // 2. 图像范围
        GridEnvelope gridRange = reader.getOriginalGridRange();
        metadataVO.setWidth(gridRange.getSpan(0));
        metadataVO.setHeight(gridRange.getSpan(1));

        // 3. 坐标系
        CoordinateReferenceSystem crs = reader.getCoordinateReferenceSystem();
        String crsName = crs.getName().toString();
        metadataVO.setCrs(crsName);

        // 4. 分辨率
        double resolutionX = envelope.getSpan(0) / gridRange.getSpan(0);
        double resolutionY = envelope.getSpan(1) / gridRange.getSpan(1);
        metadataVO.setResolutionX(resolutionX);
        metadataVO.setResolutionY(resolutionY);

        // 5. 波段数
        GridCoverage2D coverage = null;
        try {
            coverage = reader.read(null);
        } catch (IOException e) {
            return metadataVO; // 读取失败，返回已有的元数据
        }
        SampleModel sampleModel = coverage.getRenderedImage().getSampleModel();
        metadataVO.setBandCount(sampleModel.getNumBands());

        // 6. 每个波段的位深
        metadataVO.setBitDepth(Arrays.stream(sampleModel.getSampleSize())
                .boxed()
                .toList());

        return metadataVO;
    }

    /**
     * 获取文件列表
     *
     * @param pageNum  页码
     * @param pageSize 每页大小
     * @param fileName 文件名
     * @param fileType 文件类型
     * @return 文件列表
     */
    public PageResult list(Integer pageNum, Integer pageSize, String fileName, String fileType) {
        PageHelper.startPage(pageNum, pageSize);
        // 获取当前用户ID
        Integer userId = ThreadLocalUtil.getCurrentId();
        Page<GeoFile> res = geoFileMapper.page(pageNum, pageSize, fileName, fileType, userId);
        return new PageResult<GeoFile>(res.getTotal(), res.getResult());
    }

    /**
     * 预览文件
     *
     * @param id 文件id
     */
    @Override
    public void previewTiff(Integer id, HttpServletResponse response) {
        GeoFile geoFile = geoFileUtil.checkFile(id);

        // 从 MinIO 读取 GeoTIFF 文件
        GeoTiffReader reader = null;
        GridCoverage2D coverage = null;
        try (InputStream inputStream = minioUtil.getObject(minioProp.getBucketName(), geoFile.getObjectName())) {
            reader = new GeoTiffReader(inputStream);
            coverage = reader.read(null);
        } catch (Exception e) {
            throw new RuntimeException("读取文件失败");
        }
        // 渲染为图像
        RenderedImage renderedImage = coverage.getRenderedImage();
        // 写出为 PNG
        response.setContentType("image/png");
        try {
            ImageIO.write(renderedImage, "png", response.getOutputStream());
            response.flushBuffer(); // 确保及时发送
        } catch (IOException e) {
            throw new RuntimeException("写出文件失败");
        }
    }

    /**
     * 计算直方图
     * @param id 文件id
     * @param band 波段
     * @param binSize 按区间分桶bin, bin大小，如256
     * @return
     */
    @Override
    public Map<Integer, Long> computeHistogram(Integer id, Integer band, Integer binSize) {
        GeoFile geoFile = geoFileUtil.checkFile(id);

        RenderedImage renderedImg = imageUtil.getRenderedImg(minioProp.getBucketName(), geoFile.getObjectName());

        Raster raster = renderedImg.getData();
        int width = raster.getWidth();
        int height = raster.getHeight();

        Map<Integer, Long> histogram = new HashMap<>();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int value = raster.getSample(x, y, band); // 取第一个波段
                int key = value - value % binSize;
                histogram.put(key, histogram.getOrDefault(key, 0L) + 1);
            }
        }
        return histogram;
    }
}
