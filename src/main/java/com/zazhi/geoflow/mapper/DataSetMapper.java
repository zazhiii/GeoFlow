package com.zazhi.geoflow.mapper;

import com.zazhi.geoflow.entity.pojo.DataSet;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

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
    void insert(DataSet dataSet);

    /**
     * 获取数据集列表
     *
     * @param currentId 当前用户ID
     * @return 数据集列表
     */
    @Select("SELECT * FROM data_set WHERE user_id = #{currentId}")
    List<DataSet> list(Integer currentId);
}
