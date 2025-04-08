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
     * 裁剪tiff文件
     *
     * @param id 文件id
     * @param x1 裁剪范围左下角x坐标
     * @param y1 裁剪范围左下角y坐标
     * @param x2 裁剪范围右上角x坐标
     * @param y2 裁剪范围右上角y坐标
     */
    @Override
    public void cropTiff(Integer id, Integer x1, Integer y1, Integer x2, Integer y2) {
        GeoFile geoFile = geoFileMapper.getById(id);
        if (geoFile == null) {
            throw new RuntimeException("文件不存在");
        }
        // TODO 文件类型校验

        // 从minio读取文件
        InputStream inputStream = null;
        GeoTiffReader reader = null;
        GridCoverage2D gridCoverage2D = null;
        try {
            inputStream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(minioProp.getBucketName())
                            .object(geoFile.getObjectName())
                            .build()
            );
            reader = new GeoTiffReader(inputStream);
            gridCoverage2D = reader.read(null);
        } catch (Exception e) {
            throw new RuntimeException("读取文件失败");
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                throw new RuntimeException("关闭流失败");
            }
        }

        // 创建裁剪后的文件结构
        CoordinateReferenceSystem targetCRS = gridCoverage2D.getCoordinateReferenceSystem2D();
        ReferencedEnvelope referencedEnvelope = new ReferencedEnvelope(x1, x2, y1, y2, targetCRS);

        // 裁剪
        CoverageProcessor processor = CoverageProcessor.getInstance();
        ParameterValueGroup param = processor.getOperation("CoverageCrop").getParameters();
        param.parameter("Source").setValue(gridCoverage2D);
        param.parameter("Envelope").setValue(referencedEnvelope);
        GridCoverage2D finalCoverage = null;
        try {
            finalCoverage = (GridCoverage2D) processor.doOperation(param);
        } catch (EmptyIntersectionException e) {
            throw new RuntimeException("裁剪范围和原始文件范围无交集");
        }

        // 临时文件目录
        String tempDir = System.getProperty("java.io.tmpdir");
        File cropDirectory = new File(tempDir, "geoflow_crop");
        if (!cropDirectory.exists()) {
            cropDirectory.mkdirs();
        }
        // 输出文件
        String fileName = geoFile.getFileName();
        String target_file_name = fileName.substring(0, fileName.lastIndexOf('.') + 1) + "_crop.tif";
        File cropFile = new File(cropDirectory, target_file_name);
        GeoTiffFormat format = (GeoTiffFormat) reader.getFormat();
        try {
            format.getWriter(cropFile).write(finalCoverage, null);
        } catch (IOException e) {
            throw new RuntimeException("生成裁剪文件失败");
        }

        // 上传裁剪后的文件
        // 构造 objectName: 日期/uuid.后缀
        String suffix = target_file_name.substring(target_file_name.lastIndexOf(".") + 1);
        String currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String objectName = currentDate + "/" + UUID.randomUUID() + "." + suffix;
        MultipartFile multipartFile = null;
        try {
            multipartFile = new MockMultipartFile("file", cropFile.getName(), "application/octet-stream", new FileInputStream(cropFile));
            //文件名称相同会覆盖
            minioClient.putObject(
                    PutObjectArgs
                            .builder()
                            .bucket(minioProp.getBucketName())
                            .object(objectName)
                            .stream(multipartFile.getInputStream(), multipartFile.getSize(), -1)
                            .contentType(multipartFile.getContentType())
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("上传失败");
        }

        // 保存记录
        String url = minioProp.getEndpoint() + "/" + minioProp.getBucketName() + "/" + objectName;
        GeoFile cropGeoFile = GeoFile.builder()
                .userId(ThreadLocalUtil.getCurrentId())
                .fileName(target_file_name)
                .objectName(objectName)
                .url(url)
                .fileSize(multipartFile.getSize())
                .fileType(suffix)
                .build();
        geoFileMapper.insert(cropGeoFile);

        // 删除临时文件
        cropFile.delete();
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
        Page<GeoFile> res = geoFileMapper.page(pageNum, pageSize, fileName, fileType);
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

    /**
     * 合并RGB文件
     *
     * @param rid 红色通道文件id
     * @param gid 绿色通道文件id
     * @param bid 蓝色通道文件id
     */
    @Override
    public void combineRGB(Integer rid, Integer gid, Integer bid, HttpServletResponse response) {
        GeoFile RGeoFile = geoFileMapper.getById(rid);
        GeoFile GGeoFile = geoFileMapper.getById(gid);
        GeoFile BGeoFile = geoFileMapper.getById(bid);
        if (RGeoFile == null || GGeoFile == null || BGeoFile == null) {
            throw new RuntimeException("文件不存在");
        }
        Integer userId = ThreadLocalUtil.getCurrentId();
        if (!RGeoFile.getUserId().equals(userId) || !GGeoFile.getUserId().equals(userId) || !BGeoFile.getUserId().equals(userId)) {
            throw new RuntimeException("无权限查看");
        }
        // 从 MinIO 读取 GeoTIFF 文件
        RenderedImage imgR = null;
        RenderedImage imgG = null;
        RenderedImage imgB = null;
        try (InputStream isR = minioUtil.getObject(minioProp.getBucketName(), RGeoFile.getObjectName());
             InputStream isG = minioUtil.getObject(minioProp.getBucketName(), GGeoFile.getObjectName());
             InputStream isB = minioUtil.getObject(minioProp.getBucketName(), BGeoFile.getObjectName())) {
            GeoTiffReader Rreader = new GeoTiffReader(isR);
            GeoTiffReader Greader = new GeoTiffReader(isG);
            GeoTiffReader Breader = new GeoTiffReader(isB);
            GridCoverage2D covR = Rreader.read(null);
            GridCoverage2D covG = Greader.read(null);
            GridCoverage2D covB = Breader.read(null);
            imgR = covR.getRenderedImage();
            imgG = covG.getRenderedImage();
            imgB = covB.getRenderedImage();
        } catch (Exception e) {
            throw new RuntimeException("读取文件失败");
        }

        int width = imgR.getWidth();
        int height = imgR.getHeight();

        Raster rasterR = imgR.getData();
        Raster rasterG = imgG.getData();
        Raster rasterB = imgB.getData();

        BufferedImage rgb = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int r = clamp(rasterR.getSample(x, y, 0) / 256); // 简单拉伸
                int g = clamp(rasterG.getSample(x, y, 0) / 256);
                int b = clamp(rasterB.getSample(x, y, 0) / 256);

                int rgbVal = (r << 16) | (g << 8) | b;
                rgb.setRGB(x, y, rgbVal);
            }
        }

        // 设置响应头
        response.setContentType("image/png");
        try {
            ImageIO.write(rgb, "png", response.getOutputStream());
            response.flushBuffer(); // 确保及时发送
        } catch (IOException e) {
            throw new RuntimeException("写出文件失败");
        }


    }

    private int clamp(int val) {
        return Math.min(255, Math.max(0, val));
    }

    @Override
    public void loadDataset(Integer id, String name, String sensorType) {
        GeoFile geoFile = geoFileMapper.getById(id);
        if (geoFile == null) {
            throw new RuntimeException("文件不存在");
        }
        if (!geoFile.getUserId().equals(ThreadLocalUtil.getCurrentId())) {
            throw new RuntimeException("无权限查看");
        }

        // 创建数据集，保存数据集信息到数据库
        DataSet dataSet = DataSet.builder()
                .name(name)
                .userId(ThreadLocalUtil.getCurrentId())
                .sensorType(sensorType)
                .build();
        dataSetMapper.insert(dataSet);

        // 加载数据集
        String tempDir = System.getProperty("java.io.tmpdir");
        // 从 MinIO 读取压缩文件
        try (
                InputStream is = minioUtil.getObject(minioProp.getBucketName(), geoFile.getObjectName());
                GZIPInputStream gis = new GZIPInputStream(is);
                TarArchiveInputStream tis = new TarArchiveInputStream(gis)
        ) {
            // 解压缩文件到临时目录
            TarArchiveEntry entry;
            while ((entry = tis.getNextTarEntry()) != null) {
                File outputFile = new File(tempDir, entry.getName());
                if (entry.isDirectory()) { // 如果是文件夹，则创建文件夹
                    outputFile.mkdirs();
                } else {
                    outputFile.getParentFile().mkdirs(); // 创建上级目录, 确保其父目录存在
                    try (OutputStream os = new FileOutputStream(outputFile)) {
                        byte[] buffer = new byte[4096];
                        int len;
                        while ((len = tis.read(buffer)) != -1) {
                            os.write(buffer, 0, len);
                        }
                    }
                }

                // 上传文件到 MinIO
                String outputFileName = outputFile.getName();
                String suffix = outputFileName.substring(outputFileName.lastIndexOf(".") + 1);
                String currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                String objectName = currentDate + "/" + UUID.randomUUID() + "." + suffix;

                String url = null;
                try(InputStream outputIs = new FileInputStream(outputFile)){
                     url = minioUtil.upload(outputIs, outputFile.length(), objectName, Files.probeContentType(outputFile.toPath()));
                }

                // 保存文件记录到数据库
                GeoFile outputGeoFile = GeoFile.builder()
                        .userId(ThreadLocalUtil.getCurrentId())
                        .dataSetId(dataSet.getId()) // 关联数据集ID
                        .fileName(outputFile.getName())
                        .objectName(objectName)
                        .url(url)
                        .fileSize(outputFile.length())
                        .fileType(suffix)
                        .build();
                // 关联文件与数据集,
                geoFileMapper.insert(outputGeoFile);

                // 删除临时文件
                outputFile.delete();
            }
        } catch (Exception e) {
            throw new RuntimeException("加载数据集失败");
        }
    }
}
