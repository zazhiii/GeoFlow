package com.zazhi.geoflow.config;

import com.zazhi.geoflow.config.properties.MinioConfigProperties;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
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

        MinioClient minioClient = MinioClient.builder()
                .endpoint(prop.getEndpoint())
                .credentials(prop.getAccessKey(), prop.getSecretKey())
                .build();

        // 检查存储桶是否存在，不存在则创建
        try {
            Boolean found = minioClient.bucketExists(
                    BucketExistsArgs.builder()
                            .bucket(prop.getBucketName())
                            .build()
            );
            if (!found) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder()
                                .bucket(prop.getBucketName())
                                .build()
                );
            }
        } catch (Exception e) {
            throw new RuntimeException("minio初始化失败");
        }
        return minioClient;
    }
}
