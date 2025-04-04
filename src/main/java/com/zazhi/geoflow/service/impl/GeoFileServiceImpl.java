package com.zazhi.geoflow.service.impl;

import com.zazhi.geoflow.config.properties.MinioConfigProperties;
import com.zazhi.geoflow.entity.pojo.GeoFile;
import com.zazhi.geoflow.mapper.GeoFileMapper;
import com.zazhi.geoflow.service.GeoFileService;
import com.zazhi.geoflow.utils.MinioUtil;
import com.zazhi.geoflow.utils.ThreadLocalUtil;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.errors.*;
import lombok.extern.slf4j.Slf4j;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.processing.CoverageProcessor;
import org.geotools.coverage.processing.EmptyIntersectionException;
import org.geotools.data.DataSourceException;
import org.geotools.gce.geotiff.GeoTiffFormat;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.geometry.Envelope2D;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.coverage.grid.Format;
import org.opengis.coverage.processing.OperationNotFoundException;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

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
                .status(1)
                .build();
        geoFileMapper.insert(cropGeoFile);

        // 删除临时文件
        cropFile.delete();
    }
}
