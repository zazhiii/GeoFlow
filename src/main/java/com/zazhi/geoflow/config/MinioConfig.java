package com.zazhi.geoflow.config;

import com.zazhi.geoflow.config.properties.MinioConfigProperties;
import io.minio.MinioClient;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinioConfig {

    @Autowired
    private MinioConfigProperties prop;

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(prop.getEndpoint())
                .credentials(prop.getAccessKey(), prop.getSecretKey())
                .build();
    }
}
