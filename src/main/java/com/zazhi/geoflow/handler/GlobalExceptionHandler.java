package com.zazhi.geoflow.handler;

import com.zazhi.geoflow.entity.pojo.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

//    /**
//     * 处理未认证异常
//     * @param e
//     * @return
//     */
//    @ResponseStatus(HttpStatus.UNAUTHORIZED)
//    @ExceptionHandler(UnauthenticatedException.class)
//    public Result handleUnauthenticatedException(UnauthenticatedException e){
//        return Result.error("未认证或Token无效，请重新登录");
//    }
//
//    /**
//     * 处理未授权异常
//     * @param e
//     * @return
//     */
//    @ResponseStatus(HttpStatus.FORBIDDEN)
//    @ExceptionHandler(UnauthorizedException.class)
//    public Result handleUnauthorizedException(UnauthorizedException e){
//        return Result.error("未授权");
//    }


    @ExceptionHandler(Exception.class)
    public Result handleException(Exception e) {
        log.error("{}", e);
        return Result.error(StringUtils.hasLength(e.getMessage()) ? e.getMessage() : "操作失败");
    }
}