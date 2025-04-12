package com.zazhi.geoflow.service;

import com.zazhi.geoflow.entity.pojo.DataSet;

import java.util.List;

public interface DataSetService {
    /**
     * 加载数据集
     * @param id
     * @param datasetName
     * @param sensor 传感器
     */
    void loadDataset(Integer id, String name, String sensorType);

    /**
     * 获取数据集列表
     *
     * @return 数据集列表
     */
    List<DataSet> list();
}
