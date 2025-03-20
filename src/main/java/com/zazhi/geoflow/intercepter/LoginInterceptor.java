package com.zazhi.geoflow.interceptor;

import cn.hutool.jwt.JWT;
import com.zazhi.geoflow.config.properties.JWTProperties;
import com.zazhi.geoflow.utils.ThreadLocalUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.HashMap;

@Component
public class LoginInterceptor implements HandlerInterceptor {

    @Autowired
    private JWTProperties jwtProp;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //令牌验证
        String token = request.getHeader("Authorization");

//        System.out.println(request.getRequestURI());

        JWT jwt = JWT.of(token).setKey(jwtProp.getSecret().getBytes());
        Boolean validate = jwt.validate(0);

        if (!validate) {
            response.setStatus(401);
            return false;
        }

        //把业务数据存到ThreadLocal
        HashMap<String, Object> map = new HashMap<>();
        map.put("id", jwt.getPayload("id"));
        map.put("username", jwt.getPayload("username"));
        ThreadLocalUtil.set(map);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        ThreadLocalUtil.remove();
    }
}
