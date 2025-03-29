package com.zazhi.geoflow.controller;

import com.zazhi.geoflow.entity.pojo.Result;
import com.zazhi.geoflow.entity.vo.TaskInfoVO;
import com.zazhi.geoflow.service.UploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @author zazhi
 * @date 2025/3/25
 * @description: 上传
 */
@RestController
@RequestMapping("api/upload")
@Tag(name = "分片上传", description = "文件分片上传")
public class UploadController {

    @Autowired
    private UploadService uploadService;

    /**
     * 初始化上传任务
     * @param identifier
     * @param totalSize
     * @param chunkSize
     * @param fileName
     * @return
     */
    @PostMapping
    @Operation(summary = "初始化上传任务")
    public Result<TaskInfoVO> initTask(String identifier,
                                       Long totalSize,
                                       Long chunkSize,
                                       String fileName){
        return Result.success(uploadService.initTask(identifier, totalSize, chunkSize, fileName));
    }

    /**
     * 获取预签名URL
     * 前端通过该URL上传分片文件
     * ^这种方案也有一个问题, minio 服务地址暴露在前端, 有安全隐患
     * @param identifier
     * @param partNumber
     * @return
     */
    @GetMapping("/{identifier}/{partNumber}")
    @Operation(summary = "获取预签名URL")
    public Result<String> getPresignedObjectUrl(@PathVariable("identifier") String identifier,
                                                @PathVariable("partNumber") Integer partNumber){
        return Result.success(uploadService.getPresignedObjectUrl(identifier, partNumber));
    }

    /**
     * 获取上传进度
     * @param identifier 文件md5
     * @return
     */
    @GetMapping("/{identifier}")
    @Operation(summary = "获取上传进度")
    public Result<TaskInfoVO> taskInfo (@PathVariable("identifier") String identifier) {
        return Result.success(uploadService.getTaskInfo(identifier));
    }

    /**
     * 合并分片
     * @param identifier
     * @return
     */
    @PostMapping("/merge/{identifier}")
    @Operation(summary = "合并分片")
    public Result<String> merge(@PathVariable("identifier") String identifier) {
        uploadService.merge(identifier);
        return Result.success();
    }
}
