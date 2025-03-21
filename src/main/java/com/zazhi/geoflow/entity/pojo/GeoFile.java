package com.zazhi.geoflow.entity.pojo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GeoFile implements Serializable {
    /**
     * 文件ID
     */
    private Integer id;

    /**
     * 关联用户ID
     */
    private Integer userId;

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 完整存储路径
     */
    @JsonIgnore
    private String filePath;

    /**
     * 文件大小（字节 Byte）
     */
    private Long fileSize;

    /**
     * 文件类型（MIME/扩展名）
     */
    private String fileType;

    /**
     * 文件描述
     */
    private String description;

    /**
     * 文件状态（uploading, completed, failed）
     */
    private String status;

    /**
     * 上传任务ID
     */
    private String uploadTaskId;

    /**
     * 最后修改时间
     */
    private Date updateTime;

    /**
     * 上传时间
     */
    private Date createTime;
}