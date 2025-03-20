package com.zazhi.geoflow.controller;

import com.zazhi.geoflow.entity.pojo.Result;
import com.zazhi.geoflow.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

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
//
//    @Operation(summary = "用户注销")
//    @PostMapping(value = "logout")
//    public Result logout() {
//        log.info("用户注销");
//        return Result.success();
//    }
//
//    @Operation(summary = "用户信息")
//    @GetMapping(value = "info")
//    public Result info() {
//        log.info("用户信息");
//        return Result.success();
//    }
//
//    @Operation(summary = "用户列表")
//    @GetMapping(value = "list")
//    public Result list() {
//        log.info("用户列表");
//        return Result.success();
//    }
//
//    @Operation(summary = "用户删除")
//    @DeleteMapping(value = "delete")
//    public Result delete(@RequestParam Integer id) {
//        log.info("用户删除");
//        return Result.success();
//    }
//
//    @Operation(summary = "用户更新")
//    @PutMapping(value = "update")
//    public Result update(@RequestParam Integer id) {
//        log.info("用户更新");
//        return Result.success();
//    }

}