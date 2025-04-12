package com.zazhi.geoflow.utils;

import lombok.RequiredArgsConstructor;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.springframework.stereotype.Component;

import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.io.InputStream;

/**
 * @author zazhi
 * @date 2025/4/12
 * @description: TODO
 */
@Component
@RequiredArgsConstructor
public class ImageUtil {

    private final MinioUtil minioUtil;

    /**
     * 获取 RenderedImage
     *
     * @param bucketName 存储桶名
     * @param objectName 对象名
     * @return RenderedImage
     */
    public RenderedImage getRenderedImg(String bucketName, String objectName) {
        try (InputStream is = minioUtil.getObject(bucketName, objectName)) {
            GeoTiffReader reader = new GeoTiffReader(is);
            GridCoverage2D gridCoverage2D = reader.read(null);
            return gridCoverage2D.getRenderedImage();
        } catch (Exception e) {
            throw new RuntimeException("获取图片失败", e);
        }
    }

    /**
     * 获取 Raster
     *
     * @param bucketName 存储桶名
     * @param objectName 对象名
     * @return Raster
     */
    public Raster getRaster(String bucketName, String objectName) {
        return getRenderedImg(bucketName, objectName).getData();
    }

}
