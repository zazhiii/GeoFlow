package com.zazhi.geoflow.service;

import com.zazhi.geoflow.entity.vo.GeoFileMetadataVO;
import org.springframework.web.multipart.MultipartFile;

public interface GeoFileService {
    /**
     * 上传文件
     * @param file 文件
     * @return 文件路径
     */
    String upload(MultipartFile file, String objectName, String fileName, String description);

    /**
     * 删除文件
     * @param id 文件id
     */
    void delete(Integer id);


    /**
     * 裁剪tiff文件
     * @param id 文件id
     */
    void cropTiff(Integer id, Integer x1, Integer y1, Integer x2, Integer y2);

    /**
     * 获取文件元数据
     * @param id 文件id
     * @return 文件元数据
     */
    GeoFileMetadataVO getMetadata(Integer id);
}
