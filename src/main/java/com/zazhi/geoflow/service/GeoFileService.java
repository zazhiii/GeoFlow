package com.zazhi.geoflow.service;

import com.zazhi.geoflow.entity.pojo.GeoFile;
import com.zazhi.geoflow.entity.pojo.PageResult;
import com.zazhi.geoflow.entity.vo.GeoFileMetadataVO;
import jakarta.servlet.http.HttpServletResponse;
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

    /**
     * 获取文件列表
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @param fileName 文件名
     * @param fileType 文件类型
     * @return 文件列表
     */
    PageResult list(Integer pageNum, Integer pageSize, String fileName, String fileType);

    /**
     * 预览 GeoTiff 文件
     * @return
     */
    void previewTiff(Integer id, HttpServletResponse response);

    /**
     * 合并RGB文件
     * @param rid
     * @param gid
     * @param bid
     */
    void combineRGB(Integer rid, Integer gid, Integer bid, HttpServletResponse response);

    /**
     * 加载数据集
     * @param id
     * @param datasetName
     * @param sensor 传感器
     */
    void loadDataset(Integer id, String name, String sensorType);
}
