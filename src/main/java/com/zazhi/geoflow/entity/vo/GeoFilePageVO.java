package com.zazhi.geoflow.entity.vo;

import com.zazhi.geoflow.enums.FileType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * @author zazhi
 * @date 2025/5/9
 * @description: 分页对象
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GeoFilePageVO {
    private Integer id; // 文件ID
    private Integer userId; // 用户ID
    private Integer dataSetId; // 存储桶名
    private String fileName; // 文件逻辑名
    private Long fileSize; // 文件大小
    private FileType fileType; // 文件类型(拓展名)
    private Instant createTime;
}
