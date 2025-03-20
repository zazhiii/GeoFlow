package com.zazhi.geoflow.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author zazhi
 * @date 2025/3/20
 * @description: Jwt配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "jwt")
public class JWTProperties {
    private String secret;
    private Long expiration;
}
