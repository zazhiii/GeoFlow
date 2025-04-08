package com.zazhi.geoflow.service.impl;

import com.zazhi.geoflow.entity.pojo.DataSet;
import com.zazhi.geoflow.mapper.DataSetMapper;
import com.zazhi.geoflow.service.DataSetService;
import com.zazhi.geoflow.utils.ThreadLocalUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author zazhi
 * @date 2025/4/8
 * @description: TODO
 */
@Service
@RequiredArgsConstructor
public class DataSetServiceImpl implements DataSetService {

    private final DataSetMapper dataSetMapper;

    /**
     * 获取数据集列表
     *
     * @return 数据集列表
     */
    @Override
    public List<DataSet> list() {
        return dataSetMapper.list(ThreadLocalUtil.getCurrentId());
    }
}
