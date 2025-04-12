package com.zazhi.geoflow.controller;

import com.zazhi.geoflow.entity.pojo.Result;
import com.zazhi.geoflow.service.OperationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author zazhi
 * @date 2025/4/12
 * @description:
 */
@RestController
@RequestMapping("/operation")
@Tag(name = "操作", description = "操作管理")
@RequiredArgsConstructor
public class OperationController {

    private final OperationService operationService;

    @GetMapping("/ndvi")
    @Operation(summary = "获取NDVI", description = "获取NDVI")
    public void getNDVI(Integer redBandId, Integer nirBandId, HttpServletResponse response) {
        operationService.getNDVI(redBandId, nirBandId, response);
    }
}
