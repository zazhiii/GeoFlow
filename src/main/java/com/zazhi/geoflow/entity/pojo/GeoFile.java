package com.zazhi.geoflow.entity.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.zazhi.geoflow.enums.FileType;
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
    private Integer id; // 文件ID
    private Integer userId; // 用户ID
    private Integer dataSetId; // 存储桶名
    private String fileName; // 文件逻辑名
    private String objectName; // 文件存储名
    @JsonIgnore
    private String url; // 完整存储路径
    private Long fileSize; // 文件大小
    private FileType fileType; // 文件类型(拓展名)
    private String description; // 文件描述
//    private Integer status; // (0: 上传中, 1: 上传完成, 2: 上传失败)
//    private String uploadTaskId; // 上传任务ID
    private Date updateTime;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;
}