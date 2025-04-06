package com.zazhi.geoflow.entity.vo;

import lombok.Data;

/**
 * @author zazhi
 * @date 2025/4/5
 * @description: TODO
 */
@Data
public class GeoFileMetadataVO {

    private Double minX;
    private Double minY;
    private Double maxX;
    private Double maxY;
    
    private Integer width;
    private Integer height;

    private String crs;

    private Double resolutionX;
    private Double resolutionY;
}
