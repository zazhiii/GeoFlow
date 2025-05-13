package com.zazhi.geoflow.enums;

import lombok.Getter;

@Getter
public enum StretchMode {
    SIMPLE("SIMPLE", "简单拉伸"),
    LINEAR("LINEAR", "线性拉伸");

    private String value;
    private String description;

    StretchMode(String name, String description) {
        this.value = name;
        this.description = description;
    }

    public static StretchMode fromValue(String value) {
        for (StretchMode mode : StretchMode.values()) {
            if (mode.getValue().equalsIgnoreCase(value)) {
                return mode;
            }
        }
        throw new IllegalArgumentException("Unknown stretch mode: " + value);
    }
}
