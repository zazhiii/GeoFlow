package com.zazhi.geoflow.utils;

import com.zazhi.geoflow.entity.pojo.GeoFile;
import com.zazhi.geoflow.mapper.GeoFileMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author zazhi
 * @date 2025/4/2
 * @description: 文件工具类
 */
@Component
public class GeoFileUtil {

    @Autowired
    GeoFileMapper geoFileMapper;

    /**
     * 检查文件是否存在, 并且是否有权限访问
     * @param id
     * @return
     */
    public GeoFile checkFile(Integer id) {
        GeoFile geoFile = geoFileMapper.getById(id);
        if (geoFile == null) {
            throw new RuntimeException("文件不存在");
        }
        if (geoFile.getUserId() != ThreadLocalUtil.getCurrentId()) {
            throw new RuntimeException("没有权限访问该文件");
        }
        return geoFile;
    }

}
