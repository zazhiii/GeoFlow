package com.zazhi.geoflow.config;

import com.zazhi.geoflow.config.properties.MinioConfigProperties;
import com.zazhi.geoflow.minio.PearlMinioClient;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioAsyncClient;
import io.minio.MinioClient;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class MinioConfig {

    @Autowired
    private MinioConfigProperties prop;

    // 异步客户端 分片上传用到这个
    @Bean
    public PearlMinioClient pearlMinioClient() {
        return new PearlMinioClient(
                MinioAsyncClient.builder()
                .endpoint(prop.getEndpoint())
                .credentials(prop.getAccessKey(), prop.getSecretKey())
                .build()
        );
    }

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
            log.error("minio初始化失败", e);
            throw new RuntimeException("minio初始化失败");
        }
        return minioClient;
    }
}
