package com.zazhi.geoflow.service;

import com.zazhi.geoflow.entity.pojo.User;

public interface UserService {
    /**
     * 用户注册
     * @param username 用户名
     * @param password 密码
     */
    void register(String username, String password);

    /**
     * 用户登录
     * @param username 用户名
     * @param password 密码
     */
    String login(String username, String password);

    /**
     * 获取用户信息
     * @param id 用户id
     * @return 用户信息
     */
    User getUserInfo(Integer id);
}
