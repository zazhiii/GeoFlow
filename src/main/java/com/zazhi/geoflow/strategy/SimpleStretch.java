package com.zazhi.geoflow.strategy;

public class SimpleStretch implements StretchStrategy {
    @Override
    public int stretch(int value, int min, int max) {
        return clamp(value / 256);
    }

    private int clamp(int val) {
        return Math.max(0, Math.min(255, val));
    }
}
