package com.zazhi.geoflow.mapper;

import com.zazhi.geoflow.entity.pojo.UploadTask;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UploadMapper {

    /**
     * 插入上传任务
     * @param uploadTask
     */
    @Insert("insert into upload_task(user_id, upload_id, file_identifier, file_name, bucket_name, object_name, total_size, chunk_size, chunk_num) " +
            "values(#{userId}, #{uploadId}, #{fileIdentifier}, #{fileName}, #{bucketName}, #{objectName}, #{totalSize}, #{chunkSize}, #{chunkNum})")
    void insert(UploadTask uploadTask);

    /**
     * 通过 identifier 查询上传任务
     * @param identifier
     * @return
     */
    @Select("select * from upload_task where file_identifier = #{identifier}")
    UploadTask getByIdentifier(String identifier);

    /**
     * 删除上传任务
     * @param uploadTaskId
     */
    @Delete("delete from upload_task where id = #{uploadTaskId}")
    void deleteUploadTask(Integer uploadTaskId);

    /**
     * 通过 identifier 和用户id 查询上传任务
     * @param identifier
     * @param currentId
     * @return
     */
    @Select("select * from upload_task where file_identifier = #{identifier} and user_id = #{currentId}")
    UploadTask getByIdentifierAndUserId(String identifier, Integer currentId);
}
