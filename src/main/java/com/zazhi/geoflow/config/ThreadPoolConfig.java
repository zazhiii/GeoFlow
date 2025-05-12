package com.zazhi.geoflow.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
public class ThreadPoolConfig {

    @Bean("imageProcessingExecutor")
    public ThreadPoolTaskExecutor imageProcessingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 设置核心线程数
        executor.setCorePoolSize(Runtime.getRuntime().availableProcessors() + 1);

        // 设置最大线程数
        executor.setMaxPoolSize(Runtime.getRuntime().availableProcessors() * 2);

        // 设置队列容量
        executor.setQueueCapacity(100);

        // 设置线程活跃时间（秒）
        executor.setKeepAliveSeconds(60);

        // 设置线程名前缀
        executor.setThreadNamePrefix("ImageProcessing-");

        // 设置拒绝策略（由调用线程处理该任务）
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());

        executor.initialize();
        return executor;
    }
}
