package com.zazhi.geoflow.enums;

import lombok.Getter;

/**
 * @author zazhi
 * @date 2025/5/13
 * @description: 文件状态
 */
@Getter
public enum FileStatus {
    UPLOADING(0, "上传中"),
    UPLOADED(1, "已上传"),
    UPLOAD_FAILED(2, "上传失败");

    private int code;
    private String message;

    FileStatus(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public static FileStatus fromCode(int code) {
        for (FileStatus status : FileStatus.values()) {
            if (status.getCode() == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown file status: " + code);
    }
}
