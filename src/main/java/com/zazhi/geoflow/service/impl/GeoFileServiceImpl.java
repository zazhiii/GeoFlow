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
    public String upload(MultipartFile file, String objectName, String fileName, String description) {
        String url = minioUtil.upload(file, objectName);

        GeoFile geoFile = GeoFile.builder()
                .userId(ThreadLocalUtil.getCurrentId())
                .fileName(fileName)
                .objectName(objectName)
                .url(url)
                .fileSize(file.getSize())
                .fileType(url.substring(url.lastIndexOf(".")))
                .description(description)
                .build();

        geoFileMapper.insert(geoFile);
        return url;
    }

    /**
     * 删除文件
     *
     * @param id 文件id
     */
    @Override
    public void delete(Integer id) {
        GeoFile geoFile = geoFileMapper.getById(id);
        if(geoFile == null) {
            throw new RuntimeException("文件不存在");
        }
        if(!geoFile.getUserId().equals(ThreadLocalUtil.getCurrentId())) {
            throw new RuntimeException("无权限删除");
        }
        String url = geoFile.getUrl();
        String fileName = url.substring(url.lastIndexOf("/") + 1);
        // 删除minio文件 & 删除数据库记录
        minioUtil.remove(fileName);
        geoFileMapper.delete(id);
    }
}
