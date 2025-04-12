package com.zazhi.geoflow.service;

import jakarta.servlet.http.HttpServletResponse;

public interface OperationService {

    /**
     * 获取NDVI
     *
     * @param redBandId
     * @param nirBandId
     * @param response
     * @return
     */
    void getNDVI(Integer redBandId, Integer nirBandId, HttpServletResponse response);
}
