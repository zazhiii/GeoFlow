package com.zazhi.geoflow.service.impl;

import com.zazhi.geoflow.common.constant.ContentTypeConstant;
import com.zazhi.geoflow.common.constant.FileConstant;
import com.zazhi.geoflow.config.properties.MinioConfigProperties;
import com.zazhi.geoflow.entity.pojo.GeoFile;
import com.zazhi.geoflow.enums.FileType;
import com.zazhi.geoflow.mapper.GeoFileMapper;
import com.zazhi.geoflow.service.OperationService;
import com.zazhi.geoflow.utils.GeoFileUtil;
import com.zazhi.geoflow.utils.ImageUtil;
import com.zazhi.geoflow.utils.MinioUtil;
import com.zazhi.geoflow.utils.ThreadLocalUtil;
import comzazhigeoflowcommonconstant.ExtensionConstant;
import io.minio.MinioClient;
import io.swagger.v3.oas.annotations.Operation;
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
import java.util.UUID;

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

    /**
     * 获取NDVI
     * * @param redBand 红色波段
     *
     * @param nirBand  近红外波段
     * @param response
     * @return NDVI值
     */
    @Override
    @Operation(summary = "获取NDVI", description = "获取NDVI")
    public void getNDVI(Integer redBandId, Integer nirBandId, HttpServletResponse response) {
        GeoFile redBandFile = geoFileUtil.checkFile(redBandId);
        GeoFile nirBandFile = geoFileUtil.checkFile(nirBandId);

        RenderedImage redRenderedImg = imageUtil.getRenderedImg(minioProp.getBucketName(), redBandFile.getObjectName());
        RenderedImage nirRenderedImg = imageUtil.getRenderedImg(minioProp.getBucketName(), nirBandFile.getObjectName());
        Raster redRaster = redRenderedImg.getData();
        Raster nirRaster = nirRenderedImg.getData();

        int width = redRenderedImg.getWidth();
        int height = redRenderedImg.getHeight();

        BufferedImage ndviImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

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
        // 设置响应头
        response.setContentType("image/png");
        try {
            ImageIO.write(ndviImage, "png", response.getOutputStream());
            response.flushBuffer(); // 确保及时发送
        } catch (IOException e) {
            throw new RuntimeException("写出文件失败");
        }
    }

    /**
     * 合成RGB
     * @param redBondId
     * @param greenBondId
     * @param blueBondId
     * @param response
     */
    @Override
    public void combineRGB(Integer redBondId, Integer greenBondId, Integer blueBondId, HttpServletResponse response) {
        GeoFile RGeoFile = geoFileUtil.checkFile(redBondId);
        GeoFile GGeoFile = geoFileUtil.checkFile(greenBondId);
        GeoFile BGeoFile = geoFileUtil.checkFile(blueBondId);

        String bucketName = minioProp.getBucketName();
        // 从 MinIO 读取 GeoTIFF 文件
        RenderedImage imgR = imageUtil.getRenderedImg(bucketName, RGeoFile.getObjectName());
        RenderedImage imgG = imageUtil.getRenderedImg(bucketName, GGeoFile.getObjectName());
        RenderedImage imgB = imageUtil.getRenderedImg(bucketName, BGeoFile.getObjectName());

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

        // 写入临时文件并上传
        File tempFile = null;
        try {
            tempFile = File.createTempFile(UPLOAD_TEMP_FILE_NAME, "." + ExtensionConstant.PNG);
            ImageIO.write(rgb, "png", tempFile);
            uploadFileToMinio(new FileInputStream(tempFile), tempFile.length(), FileConstant.CROP_FILE_NAME, ContentTypeConstant.PNG, ExtensionConstant.PNG);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (tempFile != null && tempFile.exists()) {
                boolean delete = tempFile.delete();
                if(!delete){
                    log.error("删除临时文件失败: {}", tempFile.getAbsolutePath());
                }
            }
        }

        // 上传 BufferedImage 到 MinIO
//        try (
//                ByteArrayOutputStream os = new ByteArrayOutputStream();
//                ByteArrayInputStream is = new ByteArrayInputStream(os.toByteArray());
//        ) {
//            ImageIO.write(rgb, "png", os);
//            byte[] byteArray = os.toByteArray();
//            uploadFileToMinio(is, byteArray.length, FileConstant.CROP_FILE_NAME, ContentTypeConstant.PNG, ExtensionConstant.PNG);
//        } catch (IOException e) {
//            throw new RuntimeException("写出文件失败");
//        }

        // 设置响应头
//        response.setContentType("image/png");
//        try {
//            ImageIO.write(rgb, "png", response.getOutputStream());
//            response.flushBuffer(); // 确保及时发送
//        } catch (IOException e) {
//            throw new RuntimeException("写出文件失败");
//        }
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

    private void uploadFileToMinio(InputStream is, long objectSize, String fileName,
                                   String contentType, String extension) {

        String currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String objectName = currentDate + "/" + UUID.randomUUID() + "." + extension;

        String url = minioUtil.upload(is, objectSize, objectName, contentType);

        // 保存到数据库
        GeoFile cropGeoFile = GeoFile.builder()
                .userId(ThreadLocalUtil.getCurrentId())
                .fileName(fileName)
                .objectName(objectName)
                .url(url)
                .fileSize(objectSize)
                .fileType(FileType.fromValue(extension))
                .build();
        geoFileMapper.insert(cropGeoFile);
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
