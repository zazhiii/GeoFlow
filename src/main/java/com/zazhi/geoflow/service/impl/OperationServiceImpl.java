package com.zazhi.geoflow.service.impl;

import com.zazhi.geoflow.common.constant.ContentTypeConstant;
import com.zazhi.geoflow.config.properties.MinioConfigProperties;
import com.zazhi.geoflow.entity.pojo.GeoFile;
import com.zazhi.geoflow.enums.FileStatus;
import com.zazhi.geoflow.enums.FileType;
import com.zazhi.geoflow.enums.StretchMode;
import com.zazhi.geoflow.mapper.GeoFileMapper;
import com.zazhi.geoflow.service.OperationService;
import com.zazhi.geoflow.strategy.BandProcessor;
import com.zazhi.geoflow.strategy.LinearStretch;
import com.zazhi.geoflow.strategy.SimpleStretch;
import com.zazhi.geoflow.strategy.StretchStrategy;
import com.zazhi.geoflow.utils.GeoFileUtil;
import com.zazhi.geoflow.utils.ImageUtil;
import com.zazhi.geoflow.utils.MinioUtil;
import com.zazhi.geoflow.utils.ThreadLocalUtil;
import comzazhigeoflowcommonconstant.ExtensionConstant;
import com.zazhi.geoflow.utils.*;
import io.minio.MinioClient;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.processing.CoverageProcessor;
import org.geotools.coverage.processing.EmptyIntersectionException;
import org.geotools.gce.geotiff.GeoTiffFormat;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static com.zazhi.geoflow.common.constant.FileConstant.*;

/**
 * @author zazhi
 * @date 2025/4/12
 * @description: 图像操作
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Tag(name = "操作", description = "操作管理")
public class OperationServiceImpl implements OperationService {

    private final MinioClient minioClient;

    private final MinioUtil minioUtil;

    private final GeoFileMapper geoFileMapper;

    private final ImageUtil imageUtil;

    private final MinioConfigProperties minioProp;

    private final GeoFileUtil geoFileUtil;

    private final ThreadUtil threadUtil;

    public void getNDVI2(Integer redBandId, Integer nirBandId, HttpServletResponse response) {

        // 异步读取文件
        CompletableFuture<RenderedImage> redFuture = CompletableFuture.supplyAsync(() -> {
            GeoFile redBandFile = geoFileUtil.checkFile(redBandId);
            return imageUtil.getRenderedImg(minioProp.getBucketName(), redBandFile.getObjectName());
        });
        CompletableFuture<RenderedImage> nirFuture = CompletableFuture.supplyAsync(() -> {
            GeoFile nirBandFile = geoFileUtil.checkFile(nirBandId);
            return imageUtil.getRenderedImg(minioProp.getBucketName(), nirBandFile.getObjectName());
        });
        RenderedImage redRenderedImg = null, nirRenderedImg = null;
        try {
            redRenderedImg = redFuture.get();
            nirRenderedImg = nirFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("读取文件错误", e);
            throw new RuntimeException("读取文件错误，{}", e);
        }

        Raster redRaster = redRenderedImg.getData();
        Raster nirRaster = nirRenderedImg.getData();

        int width = redRenderedImg.getWidth();
        int height = redRenderedImg.getHeight();

        BufferedImage ndviImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        // 异步计算
        List<Future<?>> tasks = new ArrayList<>(height);

        for (int y = 0; y < height; y++) {
            final int row = y; // final 变量
            Future<?> future = threadUtil.submit(() -> {
                for (int x = 0; x < width; x++) {
                    double nir = nirRaster.getSampleDouble(x, row, 0);
                    double red = redRaster.getSampleDouble(x, row, 0);
                    double ndvi = (nir + red == 0) ? 0 : (nir - red) / (nir + red);
                    // 将NDVI值映射到颜色
                    Color color = getNDVIColor(ndvi);
                    ndviImage.setRGB(x, row, color.getRGB());
                }
            });

            tasks.add(future);
        }
        for(Future<?> task : tasks) {
            try {
                task.get(); // 等待所有任务完成
            } catch (InterruptedException | ExecutionException e) {
                log.error("NDVI计算错误", e);
                throw new RuntimeException("NDVI计算错误，{}", e);
            }
        }

        // 上传png TODO

    }

    /**
     * 计算 NDVI 值
     * @param redBandId
     * @param nirBandId
     * @param response
     */
    @Override
    public void getNDVI(Integer redBandId, Integer nirBandId, HttpServletResponse response) {
        GeoFile redBandFile = geoFileUtil.checkFile(redBandId);
        GeoFile nirBandFile = geoFileUtil.checkFile(nirBandId);

        String bucketName = minioProp.getBucketName();
        BufferedImage ndviImage = null;
        try (InputStream isRed = minioUtil.getObject(bucketName, redBandFile.getObjectName());
             InputStream isNir = minioUtil.getObject(bucketName, nirBandFile.getObjectName())) {
            GeoTiffReader redReader = new GeoTiffReader(isRed);
            GeoTiffReader nirReader = new GeoTiffReader(isNir);

            GridCoverage2D redGridCov = redReader.read(null);
            GridCoverage2D nirGridCov = nirReader.read(null);

            RenderedImage redRenderedImg = redGridCov.getRenderedImage();
            RenderedImage nirRenderedImg = nirGridCov.getRenderedImage();;

            Raster redRaster = redRenderedImg.getData();
            Raster nirRaster = nirRenderedImg.getData();

            int width = redRenderedImg.getWidth();
            int height = redRenderedImg.getHeight();

            ndviImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    double nir = nirRaster.getSampleDouble(x, y, 0);
                    double red = redRaster.getSampleDouble(x, y, 0);

                    double ndvi = (nir + red == 0) ? 0 : (nir - red) / (nir + red);
                    // 将NDVI值映射到颜色
                    Color color = getNDVIColor(ndvi);
                    ndviImage.setRGB(x, y, color.getRGB());
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("计算NDVI失败", e);
        }

        String currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String objectName = currentDate + "/" + UUID.randomUUID() + "." + ExtensionConstant.PNG;
        GeoFile geoFile = GeoFile.builder()
                .userId(ThreadLocalUtil.getCurrentId())
                .fileName(NDVI_FILE_NAME)
                .objectName(objectName)
                .fileType(FileType.PNG)
                .status(FileStatus.UPLOADING.getCode())
                .build();
        geoFileMapper.insert(geoFile);

        // 异步上传到minio
        uploadFileToMinIOAsync(ndviImage, objectName, geoFile);
    }

    /**
     * 合成RGB
     * @param redBondId
     * @param greenBondId
     * @param blueBondId
     */
    @Override
    public void combineRGB(Integer redBondId, Integer greenBondId, Integer blueBondId, String stretchMode) {
        // 异步读取文件
        String bucketName = minioProp.getBucketName();
        Integer userId = ThreadLocalUtil.getCurrentId();
        CompletableFuture<RenderedImage> redFuture = CompletableFuture.supplyAsync(() -> {
            GeoFile RGeoFile = geoFileUtil.checkFile(redBondId, userId);
            return imageUtil.getRenderedImg(bucketName, RGeoFile.getObjectName());
        });
        CompletableFuture<RenderedImage> greenFuture = CompletableFuture.supplyAsync(() -> {
            GeoFile GGeoFile = geoFileUtil.checkFile(greenBondId, userId);
            return imageUtil.getRenderedImg(bucketName, GGeoFile.getObjectName());
        });
        CompletableFuture<RenderedImage> blueFuture = CompletableFuture.supplyAsync(() -> {
            GeoFile BGeoFile = geoFileUtil.checkFile(blueBondId, userId);
            return imageUtil.getRenderedImg(bucketName, BGeoFile.getObjectName());
        });
        RenderedImage imgR = null, imgG = null, imgB = null;
        try {
            imgR = redFuture.get();
            imgG = greenFuture.get();
            imgB = blueFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("读取文件错误", e);
            throw new RuntimeException("读取文件错误，{}", e);
        }

        int width = imgR.getWidth();
        int height = imgR.getHeight();

        Raster rasterR = imgR.getData();
        Raster rasterG = imgG.getData();
        Raster rasterB = imgB.getData();

        BufferedImage rgb = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        int availableProcessors = Runtime.getRuntime().availableProcessors();
        int batchSize = height / availableProcessors;

        // 计算最值
        int minR = Integer.MAX_VALUE, maxR = Integer.MIN_VALUE;
        int minG = Integer.MAX_VALUE, maxG = Integer.MIN_VALUE;
        int minB = Integer.MAX_VALUE, maxB = Integer.MIN_VALUE;
        for(int i = 0; i < height; i ++){
            for(int j = 0; j < width; j ++){
                int r = rasterR.getSample(j, i, 0);
                int g = rasterG.getSample(j, i, 0);
                int b = rasterB.getSample(j, i, 0);
                minR = Math.min(minR, r);
                maxR = Math.max(maxR, r);
                minG = Math.min(minG, g);
                maxG = Math.max(maxG, g);
                minB = Math.min(minB, b);
                maxB = Math.max(maxB, b);
            }
        }

        // 拉伸策略
        StretchStrategy stretchStrategy;
        StretchMode mode = StretchMode.fromValue(stretchMode);
        switch (mode) {
            case SIMPLE:
                stretchStrategy = new SimpleStretch();
                break;
            case LINEAR:
                stretchStrategy = new LinearStretch();
                break;
            default:
                throw new IllegalArgumentException("不支持的拉伸模式: " + stretchMode); // 这里应该不会抛出异常, 因为已经在前面校验了
        }
        BandProcessor bandProcessor = new BandProcessor(stretchStrategy);

        // 多线程合成RGB
        List<Future<?>> tasks = new ArrayList<>(availableProcessors);

        int fMinR = minR, fMaxR = maxR, fMinG = minG, fMaxG = maxG, fMinB = minB, fMaxB = maxB;

        for(int i = 0; i < availableProcessors; i ++){
            final int startY = i * batchSize;
            final int endY = (i == availableProcessors - 1) ? height : startY + batchSize;
            tasks.add(threadUtil.submit(() -> {
                for (int y = startY; y < endY; y++) {
                    for (int x = 0; x < width; x++) {
                        int r = bandProcessor.processPixel(rasterR.getSample(x, y, 0), fMinR, fMaxR);
                        int g = bandProcessor.processPixel(rasterG.getSample(x, y, 0), fMinG, fMaxG);
                        int b = bandProcessor.processPixel(rasterB.getSample(x, y, 0), fMinB, fMaxB);
                        int rgbVal = (r << 16) | (g << 8) | b;
                        rgb.setRGB(x, y, rgbVal);
                    }
                }
            }));
        }
        for(Future<?> task : tasks) {
            try {
                task.get(); // 等待所有任务完成
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException("RGB合成错误");
            }
        }

        // 保存到数据库
        String currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String objectName = currentDate + "/" + UUID.randomUUID() + "." + ExtensionConstant.PNG;
        GeoFile geoFile = GeoFile.builder()
                .userId(ThreadLocalUtil.getCurrentId())
                .fileName(stretchMode + COMBINE_RGB_FILE_NAME)
                .objectName(objectName)
                .fileType(FileType.PNG)
                .status(FileStatus.UPLOADING.getCode())
                .build();
        geoFileMapper.insert(geoFile);

        // 异步上传到minio
        uploadFileToMinIOAsync(rgb, objectName, geoFile);
    }

    /**
     * 异步上传文件到 MinIO
     * @param rgb
     * @param objectName
     * @param geoFile
     */
    private void uploadFileToMinIOAsync(BufferedImage rgb, String objectName, GeoFile geoFile) {
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            File tempFile = null;
            try {
                tempFile = File.createTempFile(UPLOAD_TEMP_FILE_PREFIX, "." + ExtensionConstant.PNG);
                ImageIO.write(rgb, "png", tempFile);
                FileInputStream fis = new FileInputStream(tempFile);
                String url = minioUtil.upload(fis, tempFile.length(), objectName, ContentTypeConstant.PNG);
                fis.close();
                geoFile.setFileSize(tempFile.length());
                geoFile.setStatus(FileStatus.UPLOADED.getCode());
                geoFile.setUrl(url);
            } catch (Exception e) {
                geoFile.setStatus(FileStatus.UPLOAD_FAILED.getCode());
                log.error("上传文件失败", e);
                throw new RuntimeException("上传文件失败");
            } finally {
                geoFileMapper.update(geoFile);
                if (tempFile != null && tempFile.exists()) {
                    boolean delete = tempFile.delete();
                    if (!delete) {
                        log.error("删除临时文件失败: {}", tempFile.getAbsolutePath());
                    }
                }
            }
        });
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
        GeoFile geoFile = geoFileUtil.checkFile(id);
        // TODO 文件类型校验

        // 从minio读取文件
        GeoTiffReader reader = null;
        GridCoverage2D gridCoverage2D = null;
        try (InputStream inputStream = minioUtil.getObject(minioProp.getBucketName(), geoFile.getObjectName());){
            reader = new GeoTiffReader(inputStream);
            gridCoverage2D = reader.read(null);
        } catch (Exception e) {
            throw new RuntimeException("读取文件失败");
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
        String tempDir = TEMP_PATH;
        File cropDirectory = new File(TEMP_PATH, CROP_DIR);
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

        // 上传裁剪后的文件 构造 objectName: date/uuid.extension
        String extension = target_file_name.substring(target_file_name.lastIndexOf(".") + 1);
        String currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String objectName = currentDate + "/" + UUID.randomUUID() + "." + extension;

        // 上传到 MinIO
        String url = minioUtil.upload(cropFile, objectName);

        // 保存到数据库
        GeoFile cropGeoFile = GeoFile.builder()
                .userId(ThreadLocalUtil.getCurrentId())
                .fileName(target_file_name)
                .objectName(objectName)
                .url(url)
                .fileSize(cropFile.length())
                .fileType(FileType.fromValue(extension))
                .build();
        geoFileMapper.insert(cropGeoFile);
        // 删除临时文件
        cropFile.delete();
    }

    private int clamp(int val) {
        return Math.min(255, Math.max(0, val));
    }

    private Color getNDVIColor(double ndvi) {
        if (ndvi < 0.0) return new Color(0, 0, 128);             // 深蓝 -- [-1, 0)
        else if (ndvi < 0.2) return new Color(183, 183, 62, 255);      // 黄色 -- [0, 0.2)
        else if (ndvi < 0.4) return new Color(150, 209, 61);     // 浅绿 -- [0.2, 0.4)
        else if (ndvi < 0.6) return new Color(0, 128, 0);        // 绿色 -- [0.4, 0.6)
        else return new Color(0, 100, 0);                        // 深绿 -- [0.6, 1]
    }

}
