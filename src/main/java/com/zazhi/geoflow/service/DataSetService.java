package com.zazhi.geoflow.service;

import com.zazhi.geoflow.entity.pojo.DataSet;

import java.util.List;

public interface DataSetService {
    /**
     * 获取数据集列表
     *
     * @return 数据集列表
     */
    List<DataSet> list();
}
