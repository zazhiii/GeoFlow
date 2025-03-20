package com.zazhi.geoflow.controller;

import com.zazhi.geoflow.config.properties.MinioConfigProperties;
import com.zazhi.geoflow.entity.pojo.Result;
import com.zazhi.geoflow.utils.MinioUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author zazhi
 * @date 2025/3/20
 * @description: TODO
 */
@Slf4j
@RestController
@RequestMapping("api/file")
@Tag(name = "文件", description = "文件上传下载")
public class FileController {

    @Autowired
    private MinioUtil minioUtil;
    @Autowired
    private MinioConfigProperties prop;

    @Operation(summary = "查看存储bucket是否存在")
    @GetMapping("/bucket-exists")
    public Result bucketExists(@RequestParam("bucketName") String bucketName) {
        return Result.success(minioUtil.bucketExists(bucketName));
    }

    @Operation(summary = "创建存储bucket")
    @GetMapping("/make-bucket")
    public Result makeBucket(String bucketName) {
        return Result.success(minioUtil.makeBucket(bucketName));
    }

    @Operation(summary = "删除存储bucket")
    @GetMapping("/remove-bucket")
    public Result removeBucket(String bucketName) {
        return Result.success(minioUtil.removeBucket(bucketName));
    }

    @Operation(summary = "文件上传")
    @PostMapping("/upload")
    public Result<String> upload(@RequestParam("file") MultipartFile file) {
        return Result.success(minioUtil.upload(file));
    }

    @Operation(summary = "图片/视频预览")
    @GetMapping("/preview")
    public Result preview(@RequestParam("fileName") String fileName) {
        return Result.success(minioUtil.preview(fileName));
    }

    @Operation(summary = "文件下载")
    @GetMapping("/download")
    public Result download(@RequestParam("fileName") String fileName, HttpServletResponse res) {
        minioUtil.download(fileName,res);
        return Result.success();
    }

    @Operation(summary = "删除文件")
    @PostMapping("/delete")
    public Result remove(String url) {
        String objName = url.substring(url.lastIndexOf(prop.getBucketName()+"/") + prop.getBucketName().length()+1);
        minioUtil.remove(objName);
        return Result.success();
    }
}
