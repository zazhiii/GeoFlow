package com.zazhi.geoflow.service.impl;

import com.zazhi.geoflow.config.properties.MinioConfigProperties;
import com.zazhi.geoflow.entity.pojo.GeoFile;
import com.zazhi.geoflow.mapper.GeoFileMapper;
import com.zazhi.geoflow.service.OperationService;
import com.zazhi.geoflow.utils.ImageUtil;
import com.zazhi.geoflow.utils.MinioUtil;
import com.zazhi.geoflow.utils.ThreadLocalUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.io.IOException;

/**
 * @author zazhi
 * @date 2025/4/12
 * @description: 图像操作
 */
@Service
@RequiredArgsConstructor
@Tag(name = "操作", description = "操作管理")
public class OperationServiceImpl implements OperationService {

    private final MinioUtil minioUtil;

    private final GeoFileMapper geoFileMapper;

    private final ImageUtil imageUtil;

    private final MinioConfigProperties minioProp;

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
        GeoFile redBandFile = geoFileMapper.getById(redBandId);
        GeoFile nirBandFile = geoFileMapper.getById(nirBandId);
        if (redBandFile == null || nirBandFile == null) {
            throw new IllegalArgumentException("红色波段或近红外波段不存在");
        }
        Integer userId = ThreadLocalUtil.getCurrentId();
        if(redBandFile.getUserId() != userId || nirBandFile.getUserId() != userId) {
            throw new IllegalArgumentException("没有权限访问该文件");
        }

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
    private Color getNDVIColor(double ndvi) {
        if (ndvi < 0.0) return new Color(0, 0, 128);             // 深蓝 -- [-1, 0)
        else if (ndvi < 0.2) return new Color(183, 183, 62, 255);      // 黄色 -- [0, 0.2)
        else if (ndvi < 0.4) return new Color(150, 209, 61);     // 浅绿 -- [0.2, 0.4)
        else if (ndvi < 0.6) return new Color(0, 128, 0);        // 绿色 -- [0.4, 0.6)
        else return new Color(0, 100, 0);                        // 深绿 -- [0.6, 1]
    }

}
