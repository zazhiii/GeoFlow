package com.zazhi.geoflow.strategy;

public class LinearStretch implements StretchStrategy {
    @Override
    public int stretch(int value, int min, int max) {
        if (max == min) return 0;
        return Math.max(0, Math.min(255, (value - min) * 255 / (max - min)));
    }
}
