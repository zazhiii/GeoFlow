package com.zazhi.geoflow.strategy;

public class BandProcessor {
    private StretchStrategy strategy;

    public BandProcessor(StretchStrategy strategy) {
        this.strategy = strategy;
    }

    public int processPixel(int value, int min, int max) {
        return strategy.stretch(value, min, max);
    }
}
