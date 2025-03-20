package com.zazhi.geoflow.entity.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户实体类
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class User implements Serializable {
    private Integer id; // 用户ID
    private String username; // 用户名
    private String password; // 密码
    private String avatar; // 头像
    private String email; // 邮箱地址
    private String phoneNumber; // 电话号码
    private Date lastLoginTime; // 上次登录时间
    private Date updateTime; // 更新时间
    private Date createTime; // 创建时间
}
