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

    /**
     * 合成RGB
     * @param redBondId
     * @param greenBondId
     * @param blueBondId
     * @param response
     */
    void combineRGB(Integer redBondId, Integer greenBondId, Integer blueBondId, String stretchMode);

    /**
     * 裁剪tiff文件
     * @param id 文件id
     */
    void cropTiff(Integer id, Integer x1, Integer y1, Integer x2, Integer y2);
}
