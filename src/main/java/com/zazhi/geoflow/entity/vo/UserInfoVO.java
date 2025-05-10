package com.zazhi.geoflow.entity.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.Instant;
import java.util.Date;

/**
 * @author zazhi
 * @date 2025/5/10
 * @description: 用户信息
 */
public class UserInfoVO {
    private Integer id; // 用户ID
    private String username; // 用户名
    private String avatar; // 头像
    private String email; // 邮箱地址
    private Long totalFileSize; // 上传文件总大小
    private Instant createTime; // 创建时间
}
