package com.zazhi.geoflow.utils;

import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.*;

@Component
public class ThreadUtil {

    @Resource
    @Qualifier("imageProcessingExecutor")
    private ThreadPoolTaskExecutor executor;

    /**
     * 提交一个带返回值的任务
     */
    public <T> Future<T> submit(Callable<T> task) {
        return executor.submit(task);
    }

    /**
     * 提交一个无返回值的任务
     */
    public Future<?> submit(Runnable task) {
        return executor.submit(task);
    }

    /**
     * 执行一个无返回值的任务
     */
    public void execute(Runnable task) {
        executor.execute(task);
    }

}
