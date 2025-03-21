package com.zazhi.geoflow.service.impl;

import com.zazhi.geoflow.entity.pojo.GeoFile;
import com.zazhi.geoflow.mapper.GeoFileMapper;
import com.zazhi.geoflow.service.GeoFileService;
import com.zazhi.geoflow.utils.MinioUtil;
import com.zazhi.geoflow.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author zazhi
 * @date 2025/3/20
 * @description: 文件
 */
@Service
public class GeoFileServiceImpl implements GeoFileService {

    @Autowired
    private MinioUtil minioUtil;

    @Autowired
    private GeoFileMapper geoFileMapper;
    /**
     * 上传文件
     *
     * @param file 文件
     * @return 文件路径
     */
    @Override
    public String upload(MultipartFile file, String fileName, String description) {
        String url = minioUtil.upload(file);

        GeoFile geoFile = GeoFile.builder()
                .userId(ThreadLocalUtil.getCurrentId())
                .fileName(fileName)
                .filePath(url)
                .fileSize(file.getSize())
                .fileType(url.substring(url.lastIndexOf(".")))
                .description(description)
                .build();

        geoFileMapper.insert(geoFile);
        return url;
    }
}
