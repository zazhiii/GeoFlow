package com.zazhi.geoflow.service.impl;

import ch.qos.logback.core.util.MD5Util;
import cn.hutool.crypto.SecureUtil;
import com.zazhi.geoflow.entity.pojo.User;
import com.zazhi.geoflow.mapper.UserMapper;
import com.zazhi.geoflow.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author zazhi
 * @date 2025/3/20
 * @description: 用户
 */
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    /**
     * 用户注册
     * @param username 用户名
     * @param password 密码
     */
    @Override
    public void register(String username, String password) {
        User user = userMapper.getUserByUsername(username);
        if(user != null) {
            throw new RuntimeException("用户已存在");
        }

        user = User.builder()
                .username(username)
                .password(SecureUtil.md5(password))
                .build();
        userMapper.insert(user);
    }
}
