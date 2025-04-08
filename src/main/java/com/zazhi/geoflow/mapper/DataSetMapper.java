package com.zazhi.geoflow.mapper;

import com.zazhi.geoflow.entity.pojo.DataSet;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author zazhi
 * @date 2025/4/8
 * @description: 数据集表 Mapper 接口
 */
@Mapper
public interface DataSetMapper {

    /**
     * 插入数据集
     *
     * @param dataSet 数据集
     */
//    @Insert("INSERT INTO dataset (name, user_id, sensor_type) " +
//            "VALUES (#{name}, #{userId}, #{sensorType})")
    void insert(DataSet dataSet);
}
