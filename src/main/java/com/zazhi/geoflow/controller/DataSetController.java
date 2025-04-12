package com.zazhi.geoflow.controller;

import com.zazhi.geoflow.entity.pojo.DataSet;
import com.zazhi.geoflow.entity.pojo.Result;
import com.zazhi.geoflow.service.DataSetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author zazhi
 * @date 2025/4/8
 * @description: TODO
 */
@RestController
@RequestMapping("/dataset")
@Tag(name = "数据集", description = "数据集管理")
@Slf4j
@RequiredArgsConstructor
public class DataSetController {

    private final DataSetService dataSetService;

    @Operation(summary = "数据集列表", description = "数据集列表")
    @GetMapping("/list")
    public Result<List<DataSet>> list() {
        log.info("数据集列表");
        return Result.success(dataSetService.list());
    }

    @Operation(summary = "加载数据集")
    @GetMapping("/load-dataset")
    public Result loadDataset(
            @RequestParam("id") Integer id,
            @RequestParam("name") String name,
            @RequestParam("sensorType") String sensorType
    ) {
        log.info("加载数据集");
        dataSetService.loadDataset(id, name, sensorType);
        return Result.success();
    }
}
