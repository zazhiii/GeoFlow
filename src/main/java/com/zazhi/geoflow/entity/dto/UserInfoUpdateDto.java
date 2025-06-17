package com.zazhi.geoflow.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserInfoUpdateDto {
    private String username; // 用户名
    private String email; // 邮箱地址
    private String avatar; // 头像
}
