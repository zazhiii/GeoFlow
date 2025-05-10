package com.zazhi.geoflow.controller;

import com.zazhi.geoflow.entity.pojo.GeoFile;
import com.zazhi.geoflow.entity.pojo.Result;
import com.zazhi.geoflow.entity.pojo.User;
import com.zazhi.geoflow.service.UserService;
import com.zazhi.geoflow.utils.ThreadLocalUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.websocket.server.PathParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * @author zazhi
 * @date 2025/3/20
 * @description: 用户
 */
@Slf4j
@RestController
@RequestMapping("api/user")
@Tag(name = "用户")
public class UserController {

    @Autowired
    private UserService userService;

    @Operation(summary = "用户注册")
    @PostMapping(value = "register")
    public Result register(@RequestParam String username, @RequestParam String password) {
        log.info("用户注册");

        userService.register(username, password);
        return Result.success();
    }

    @Operation(summary = "用户登录")
    @PostMapping(value = "login")
    public Result<String> login(@RequestParam String username, @RequestParam String password) {
        log.info("用户登录");

        return Result.success(userService.login(username, password));
    }

    @Operation(summary = "当前用户信息")
    @GetMapping(value = "info")
    public Result<User> info() {
        log.info("当前用户信息");
        return Result.success(userService.getUserInfo(ThreadLocalUtil.getCurrentId()));
    }

    @Operation(summary = "用户信息")
    @GetMapping(value = "info/{id}")
    public Result<User> info(@PathVariable("id") Integer id) {
        log.info("用户信息");
        return Result.success(userService.getUserInfo(id));
    }

    @Operation(summary = "上传头像")
    @PostMapping(value = "uploadAvatar")
    public Result uploadAvatar(@RequestParam("file") MultipartFile file) {
        log.info("上传头像");
        userService.uploadAvatar(file);
        return Result.success();
    }

}