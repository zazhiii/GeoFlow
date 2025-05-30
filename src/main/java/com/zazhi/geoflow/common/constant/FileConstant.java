package com.zazhi.geoflow.common.constant;

import java.nio.file.FileSystems;
import java.util.UUID;

/**
 * @author zazhi
 * @date 2025/5/11
 * @description: 路径常量
 */
public class FileConstant {
    public static final String TEMP_PATH = System.getProperty("java.io.tmpdir") + System.getProperty("separator") + "geoflow" + FileSystems.getDefault().getSeparator();
    public static final String CROP_DIR = "crop";

    public static final String CROP_FILE_NAME = "crop-" + UUID.randomUUID().toString();
    public static final String COMBINE_RGB_FILE_NAME = "combineRGB-" + UUID.randomUUID().toString();

    public static final String UPLOAD_TEMP_FILE_PREFIX = "geoflow-upload-";

    public static final String NDVI_FILE_NAME = "ndvi-" + UUID.randomUUID().toString();
}
