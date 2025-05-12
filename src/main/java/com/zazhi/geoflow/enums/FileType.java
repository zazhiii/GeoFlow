package com.zazhi.geoflow.enums;

import lombok.Getter;

public enum FileType {
    TIFF("tif"),
    GZIP("gz"),
    PNG("png");

    @Getter
    private String extension;

    FileType(String extension) {
        this.extension = extension;
    }

    public static FileType fromValue(String value) {
        for (FileType fileType : FileType.values()) {
            if (fileType.getExtension().equalsIgnoreCase(value)) {
                return fileType;
            }
        }
        throw new IllegalArgumentException("Unknown file type: " + value);
    }

}
