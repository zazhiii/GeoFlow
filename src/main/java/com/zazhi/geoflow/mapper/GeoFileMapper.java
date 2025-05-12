package com.zazhi.geoflow.mapper;

import com.github.pagehelper.Page;
import com.zazhi.geoflow.entity.pojo.GeoFile;
import com.zazhi.geoflow.entity.vo.GeoFilePageVO;
import org.apache.ibatis.annotations.*;

@Mapper
public interface GeoFileMapper {
    /**
     * 插入文件
     * @param geoFile 文件
     */
    @Insert("insert into geo_file(user_id, file_name, object_name, url, file_size, file_type, description, upload_task_id) " +
            "values(#{userId}, #{fileName}, #{objectName}, #{url}, #{fileSize}, #{fileType}, #{description}, #{uploadTaskId})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(GeoFile geoFile);

    /**
     * 根据ID查询文件
     * @param id 文件ID
     * @return 文件
     */
    @Select("select * from geo_file where id = #{id}")
    GeoFile getById(Integer id);

    /**
     * 删除文件
     * @param id 文件ID
     */
    @Delete("delete from geo_file where id = #{id}")
    void delete(Integer id);

    /**
     * 根据对象名查询文件
     * @param objectName 对象名
     * @return 文件
     */
    @Select("select * from geo_file where object_name = #{objectName}")
    GeoFile getByObjectName(String objectName);

    /**
     * 根据用户ID查询文件
     * @param userId 用户ID
     * @return 文件
     */
    Page<GeoFilePageVO> page(Integer pageNum, Integer pageSize, String fileName, String fileType, Integer userId);

    /**
     * 更新文件
     * @param geoFile 文件
     */
    void update(GeoFile geoFile);
}
