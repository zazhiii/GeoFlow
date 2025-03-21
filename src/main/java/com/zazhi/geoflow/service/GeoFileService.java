package com.zazhi.geoflow.service;

import org.springframework.web.multipart.MultipartFile;

public interface GeoFileService {
    /**
     * 上传文件
     * @param file 文件
     * @return 文件路径
     */
    String upload(MultipartFile file, String fileName, String description);

    /**
     * 删除文件
     * @param id 文件id
     */
    void delete(Integer id);
}
