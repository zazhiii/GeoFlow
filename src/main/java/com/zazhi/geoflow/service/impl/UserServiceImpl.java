package com.zazhi.geoflow.service.impl;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.jwt.JWT;
import com.zazhi.geoflow.config.properties.JWTProperties;
import com.zazhi.geoflow.entity.pojo.User;
import com.zazhi.geoflow.mapper.UserMapper;
import com.zazhi.geoflow.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * @author zazhi
 * @date 2025/3/20
 * @description: 用户
 */
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JWTProperties jwtProperties;

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

    /**
     * 用户登录
     * @param username 用户名
     * @param password 密码
     */
    @Override
    public String login(String username, String password) {
        User user = userMapper.getUserByUsername(username);
        if(user == null) {
            throw new RuntimeException("用户不存在");
        }
        if(!user.getPassword().equals(SecureUtil.md5(password))) {
            throw new RuntimeException("密码错误");
        }

        // 生成 token
        return JWT.create()
                .setPayload("id", user.getId())
                .setPayload("username", user.getUsername())
                .setKey(jwtProperties.getSecret().getBytes())
                .setExpiresAt(new Date(System.currentTimeMillis() + jwtProperties.getExpiration()))
                .sign();
    }

    /**
     * 获取用户信息
     * @param id 用户id
     * @return 用户信息
     */
    @Override
    public User getUserInfo(Integer id) {
        return userMapper.getUserById(id);
    }
}
