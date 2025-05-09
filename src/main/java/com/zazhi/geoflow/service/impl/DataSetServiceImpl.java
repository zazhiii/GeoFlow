package com.zazhi.geoflow.service.impl;

import com.zazhi.geoflow.config.properties.MinioConfigProperties;
import com.zazhi.geoflow.entity.pojo.DataSet;
import com.zazhi.geoflow.entity.pojo.GeoFile;
import com.zazhi.geoflow.enums.FileType;
import com.zazhi.geoflow.mapper.DataSetMapper;
import com.zazhi.geoflow.mapper.GeoFileMapper;
import com.zazhi.geoflow.service.DataSetService;
import com.zazhi.geoflow.utils.MinioUtil;
import com.zazhi.geoflow.utils.ThreadLocalUtil;
import lombok.RequiredArgsConstructor;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.zip.GZIPInputStream;

/**
 * @author zazhi
 * @date 2025/4/8
 * @description: 数据集服务实现类
 */
@Service
@RequiredArgsConstructor
public class DataSetServiceImpl implements DataSetService {

    private final DataSetMapper dataSetMapper;

    private final GeoFileMapper geoFileMapper;

    private final MinioUtil minioUtil;

    private final MinioConfigProperties minioProp;

    /**
     * 获取数据集列表
     *
     * @return 数据集列表
     */
    @Override
    public List<DataSet> list() {
        return dataSetMapper.list(ThreadLocalUtil.getCurrentId());
    }

    /**
     * 加载数据集
     * @param id
     * @param name
     * @param sensorType
     */
    @Override
    public void loadDataset(Integer id, String name, String sensorType) {
        GeoFile geoFile = geoFileMapper.getById(id);
        if (geoFile == null) {
            throw new RuntimeException("文件不存在");
        }
        if (!geoFile.getUserId().equals(ThreadLocalUtil.getCurrentId())) {
            throw new RuntimeException("无权限查看");
        }

        // 创建数据集，保存数据集信息到数据库
        DataSet dataSet = DataSet.builder()
                .name(name)
                .userId(ThreadLocalUtil.getCurrentId())
                .sensorType(sensorType)
                .build();
        dataSetMapper.insert(dataSet);

        // 加载数据集
        String tempDir = System.getProperty("java.io.tmpdir");
        // 从 MinIO 读取压缩文件
        try (
                InputStream is = minioUtil.getObject(minioProp.getBucketName(), geoFile.getObjectName());
                GZIPInputStream gis = new GZIPInputStream(is);
                TarArchiveInputStream tis = new TarArchiveInputStream(gis)
        ) {
            // 解压缩文件到临时目录
            TarArchiveEntry entry;
            while ((entry = tis.getNextTarEntry()) != null) {
                File outputFile = new File(tempDir, entry.getName());
                if (entry.isDirectory()) { // 如果是文件夹，则创建文件夹
                    outputFile.mkdirs();
                } else {
                    outputFile.getParentFile().mkdirs(); // 创建上级目录, 确保其父目录存在
                    try (OutputStream os = new FileOutputStream(outputFile)) {
                        byte[] buffer = new byte[4096];
                        int len;
                        while ((len = tis.read(buffer)) != -1) {
                            os.write(buffer, 0, len);
                        }
                    }
                }

                // 上传文件到 MinIO
                String outputFileName = outputFile.getName();
                String extension = outputFileName.substring(outputFileName.lastIndexOf(".") + 1);
                String currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                String objectName = currentDate + "/" + UUID.randomUUID() + "." + extension;

                String url = null;
                try(InputStream outputIs = new FileInputStream(outputFile)){
                    url = minioUtil.upload(outputIs, outputFile.length(), objectName, Files.probeContentType(outputFile.toPath()));
                }

                // 保存文件记录到数据库
                GeoFile outputGeoFile = GeoFile.builder()
                        .userId(ThreadLocalUtil.getCurrentId())
                        .dataSetId(dataSet.getId()) // 关联数据集ID
                        .fileName(outputFile.getName())
                        .objectName(objectName)
                        .url(url)
                        .fileSize(outputFile.length())
                        .fileType(FileType.valueOf(extension))
                        .build();
                // 关联文件与数据集,
                geoFileMapper.insert(outputGeoFile);

                // 删除临时文件
                outputFile.delete();
            }
        } catch (Exception e) {
            throw new RuntimeException("加载数据集失败");
        }
    }
}
