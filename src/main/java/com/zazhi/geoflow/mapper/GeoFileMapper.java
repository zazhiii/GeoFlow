package com.zazhi.geoflow.mapper;

import com.zazhi.geoflow.entity.pojo.GeoFile;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface GeoFileMapper {
    /**
     * 插入文件
     * @param geoFile 文件
     */
    @Insert("insert into geo_file(user_id, file_name, file_path, file_size, file_type, description) values(#{userId}, #{fileName}, #{filePath}, #{fileSize}, #{fileType}, #{description})")
    void insert(GeoFile geoFile);
}
