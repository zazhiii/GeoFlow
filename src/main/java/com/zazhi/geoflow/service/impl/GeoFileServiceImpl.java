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
import com.zazhi.geoflow.utils.MinioUtil;
import com.zazhi.geoflow.utils.ThreadLocalUtil;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import jakarta.servlet.http.HttpServletResponse;
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
import java.io.*;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
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
public class GeoFileServiceImpl implements GeoFileService {

    @Autowired
    private MinioUtil minioUtil;

    @Autowired
    private GeoFileMapper geoFileMapper;

    @Autowired
    private MinioClient minioClient;

    @Autowired
    private MinioConfigProperties minioProp;

    @Autowired
    private DataSetMapper dataSetMapper;

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
                .fileType(url.substring(url.lastIndexOf(".")))
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
        GeoFile geoFile = geoFileMapper.getById(id);
        if (geoFile == null) {
            throw new RuntimeException("文件不存在");
        }
        if (!geoFile.getUserId().equals(ThreadLocalUtil.getCurrentId())) {
            throw new RuntimeException("无权限删除");
        }
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
        GeoFile geoFile = geoFileMapper.getById(id);
        if (geoFile == null) {
            throw new RuntimeException("文件不存在");
        }
        if (!geoFile.getUserId().equals(ThreadLocalUtil.getCurrentId())) {
            throw new RuntimeException("无权限查看");
        }

        // 从minio读取文件
        GeoTiffReader reader = null;
        try (InputStream inputStream = minioUtil.getObject(minioProp.getBucketName(), geoFile.getObjectName())) {
            reader = new GeoTiffReader(inputStream);
        } catch (Exception e) {
            throw new RuntimeException("读取文件失败");
        }

        // 获取文件元数据
        GeoFileMetadataVO geoFileMetadataVO = new GeoFileMetadataVO();
        // 1. 地理范围
        GeneralEnvelope envelope = reader.getOriginalEnvelope();
        geoFileMetadataVO.setMinX(envelope.getMinimum(0));
        geoFileMetadataVO.setMinY(envelope.getMinimum(1));
        geoFileMetadataVO.setMaxX(envelope.getMaximum(0));
        geoFileMetadataVO.setMaxY(envelope.getMaximum(1));

        // 2. 图像范围
        GridEnvelope gridRange = reader.getOriginalGridRange();
        geoFileMetadataVO.setWidth(gridRange.getSpan(0));
        geoFileMetadataVO.setHeight(gridRange.getSpan(1));

        // 3. 坐标系
        CoordinateReferenceSystem crs = reader.getCoordinateReferenceSystem();
        String crsName = crs.getName().toString();
        geoFileMetadataVO.setCrs(crsName);

        // 4. 分辨率
        double resolutionX = envelope.getSpan(0) / gridRange.getSpan(0);
        double resolutionY = envelope.getSpan(1) / gridRange.getSpan(1);
        geoFileMetadataVO.setResolutionX(resolutionX);
        geoFileMetadataVO.setResolutionY(resolutionY);

        return geoFileMetadataVO;
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
        GeoFile geoFile = geoFileMapper.getById(id);
        if (geoFile == null) {
            throw new RuntimeException("文件不存在");
        }
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
}
