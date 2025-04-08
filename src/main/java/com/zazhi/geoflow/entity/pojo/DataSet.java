package com.zazhi.geoflow.entity.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * @author zazhi
 * @date 2025/4/8
 * @description: 数据集实体类
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DataSet {
    private Integer id;
    private String name;
    private Integer userId;
    private String sensorType;
    private Date updateTime;
    private Date createTime;
}
