package com.zazhi.geoflow.utils;

import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.*;

@Component
public class ThreadUtil {

    @Resource
    @Qualifier("imageProcessingExecutor")
    private ExecutorService executor;

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

    /**
     * 提交多个任务
     */
    public <T> List<Future<T>> invokeAll(List<Callable<T>> tasks) {
        try {
            return executor.invokeAll(tasks);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("线程执行被中断", e);
        }
    }

    /**
     * 提交多个任务并等待全部完成（带超时）
     */
    public <T> List<Future<T>> invokeAll(List<Callable<T>> tasks, long timeout, TimeUnit unit) {
        try {
            return executor.invokeAll(tasks, timeout, unit);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("线程执行被中断", e);
        }
    }
}
