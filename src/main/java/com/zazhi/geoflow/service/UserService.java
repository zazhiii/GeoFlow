package com.zazhi.geoflow.service;

import com.zazhi.geoflow.entity.pojo.User;
import org.springframework.web.multipart.MultipartFile;

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

    /**
     * 上传头像
     * @param file 头像文件
     */
    void uploadAvatar(MultipartFile file);

}
