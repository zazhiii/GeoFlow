package com.zazhi.geoflow.entity.vo;

import lombok.Data;
import java.util.List;

/**
 * @author zazhi
 * @date 2025/4/5
 * @description: TODO
 */
@Data
public class GeoFileMetadataVO {

    // 地理范围
    private Double minX;
    private Double minY;
    private Double maxX;
    private Double maxY;
    // 图像范围
    private Integer width;
    private Integer height;
    // 坐标系
    private String crs;
    // 分辨率
    private Double resolutionX;
    private Double resolutionY;
    private Integer bandCount; // 波段数
    private List<Integer> bitDepth; // 位深
}
