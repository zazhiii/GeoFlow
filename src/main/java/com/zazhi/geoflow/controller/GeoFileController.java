package com.zazhi.geoflow.controller;

import com.zazhi.geoflow.config.properties.MinioConfigProperties;
import com.zazhi.geoflow.entity.pojo.GeoFile;
import com.zazhi.geoflow.entity.pojo.PageResult;
import com.zazhi.geoflow.entity.pojo.Result;
import com.zazhi.geoflow.entity.vo.GeoFileMetadataVO;
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

import java.util.Map;

/**
 * @author zazhi
 * @date 2025/3/20
 * @description: 文件
 */
@Slf4j
@RestController
@RequestMapping("api/geo-file")
@Tag(name = "文件", description = "文件管理")
public class GeoFileController {

    @Autowired
    private MinioUtil minioUtil;

    @Autowired
    private MinioConfigProperties prop;

    @Autowired
    private GeoFileService geoFileService;


    @Operation(summary = "预览 GeoTiff 文件")
    @GetMapping(value = "preview/tiff/{id}")
    public void previewTiff(@PathVariable("id") Integer id, HttpServletResponse response) {
        geoFileService.previewTiff(id, response);
    }

    @Operation(summary = "获取文件列表")
    @GetMapping("/list")
    public Result<PageResult<GeoFile>> list(
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
            @RequestParam(value = "fileName", required = false) String fileName,
            @RequestParam(value = "fileType", required = false) String fileType
    ) {
        return Result.success(geoFileService.list(pageNum, pageSize, fileName, fileType));
    }

    @Operation(summary = "文件直接上传")
    @PostMapping("/upload")
    public Result<String> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("fileName") String fileName,
            @RequestParam("objectName") String objectName,
            @PathParam("description") String description
    ) {
        log.info("文件上传");
        return Result.success(geoFileService.upload(file, objectName, fileName, description));
    }

    @Operation(summary = "删除文件")
    @DeleteMapping("/delete")
    public Result delete(@RequestParam("id") Integer id) {
        geoFileService.delete(id);
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

    @Operation(summary = "获取元数据")
    @GetMapping("/get-metadata")
    public Result<GeoFileMetadataVO> getMetadata(@RequestParam("id") Integer id) {
        return Result.success(geoFileService.getMetadata(id));
    }

    @Operation(summary = "计算直方图")
    @GetMapping("/compute-histogram")
    public Result<Map<Integer, Long>> computeHistogram(
            Integer id,
            @RequestParam(required = false, defaultValue = "0")Integer band,
            @RequestParam(required = false, defaultValue = "1") Integer binSize) {
        return Result.success(geoFileService.computeHistogram(id, band, binSize));
    }



}
