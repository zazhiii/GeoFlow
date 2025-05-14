package com.zazhi.geoflow.controller;

import com.zazhi.geoflow.entity.pojo.Result;
import com.zazhi.geoflow.service.OperationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * @author zazhi
 * @date 2025/4/12
 * @description:
 */
@RestController
@RequestMapping("/api/operation")
@Tag(name = "操作", description = "操作管理")
@RequiredArgsConstructor
public class OperationController {

    private final OperationService operationService;

    @Operation(summary = "裁剪tiff文件")
    @PostMapping("/crop-tiff")
    public Result cropTiff(
            Integer id,
            Integer x1,
            Integer y1,
            Integer x2,
            Integer y2
    ) {
        operationService.cropTiff(id, x1, y1, x2, y2);
        return Result.success();
    }

    @Operation(summary = "彩色合成")
    @GetMapping("/combineRGB")
    public void combineRGB(
            @RequestParam("redBondId") Integer redBondId,
            @RequestParam("greenBondId") Integer greenBondId,
            @RequestParam("blueBondId") Integer blueBondId,
            @RequestParam(value = "stretchMode", defaultValue = "SIMPLE") String stretchMode
    ){
        operationService.combineRGB(redBondId, greenBondId, blueBondId, stretchMode);
    }

    @GetMapping("/ndvi")
    @Operation(summary = "获取NDVI", description = "获取NDVI")
    public void getNDVI(Integer redBandId, Integer nirBandId, HttpServletResponse response) {
        operationService.getNDVI(redBandId, nirBandId, response);
    }
}
