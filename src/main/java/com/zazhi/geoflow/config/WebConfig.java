package com.zazhi.geoflow.config;

import com.zazhi.geoflow.interceptor.LoginInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private LoginInterceptor loginInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        //登录接口和注册接口放行
        registry.addInterceptor(loginInterceptor)
                .excludePathPatterns(
                        "/api/user/login",
                        "/api/user/register",
                        // swagger 的请求
                        "/swagger-resources/**",
                        "/webjars/**",
                        "/v3/**",
                        "/swagger-ui.html/**",
                        "/api",
                        "/api-docs",
                        "/api-docs/**",
                        "/doc.html/**",

                        "/favicon.ico",
                        "/error"
                );
    }
}
