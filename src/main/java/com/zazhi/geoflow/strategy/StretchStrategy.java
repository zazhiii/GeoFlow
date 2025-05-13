package com.zazhi.geoflow.strategy;

/**
 * @author zazhi
 * @date 2025/5/11
 * @description: 拉伸策略接口
 */
public interface StretchStrategy {
    int stretch(int value, int min, int max);
}

