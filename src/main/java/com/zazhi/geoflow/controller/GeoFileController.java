package com.zazhi.geoflow.controller;

import com.zazhi.geoflow.config.properties.MinioConfigProperties;
import com.zazhi.geoflow.entity.pojo.Result;
import com.zazhi.geoflow.service.GeoFileService;
import com.zazhi.geoflow.utils.MinioUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.websocket.server.PathParam;
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
public class GeoFileController {

    @Autowired
    private MinioUtil minioUtil;

    @Autowired
    private MinioConfigProperties prop;

    @Autowired
    private GeoFileService fileService;

    @Operation(summary = "文件直接上传")
    @PostMapping("/upload")
    public Result<String> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("fileName") String fileName,
            @RequestParam("objectName") String objectName,
            @PathParam("description") String description
    ) {
        log.info("文件上传");
        return Result.success(fileService.upload(file, objectName, fileName, description));
    }

    @Operation(summary = "删除文件")
    @DeleteMapping("/delete")
    public Result delete(@RequestParam("id") Integer id) {
        fileService.delete(id);
        return Result.success();
    }

    @Operation(summary = "图片/视频预览")
    @GetMapping("/preview")
    public Result preview(@RequestParam("fileName") String fileName) {
        return Result.success(minioUtil.preview(fileName));
    }

    @Operation(summary = "文件下载")
    @GetMapping("/download")
    public Result download(@RequestParam("fileName") String fileName, HttpServletResponse res) {
        minioUtil.download(fileName, res);
        return Result.success();
    }
}
