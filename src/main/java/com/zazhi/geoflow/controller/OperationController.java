package com.zazhi.geoflow.controller;

import com.zazhi.geoflow.entity.pojo.Result;
import com.zazhi.geoflow.service.OperationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
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
    @Parameters({
            @Parameter(name = "id", description = "文件ID", required = true, in = ParameterIn.QUERY),
            @Parameter(name = "x1", description = "左下角X地理坐标", required = true, in = ParameterIn.QUERY),
            @Parameter(name = "y1", description = "左下角Y地理坐标", required = true, in = ParameterIn.QUERY),
            @Parameter(name = "x2", description = "右上角X地理坐标", required = true, in = ParameterIn.QUERY),
            @Parameter(name = "y2", description = "右上角Y地理坐标", required = true, in = ParameterIn.QUERY)
    })
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
