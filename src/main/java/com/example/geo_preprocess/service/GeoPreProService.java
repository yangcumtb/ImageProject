package com.example.geo_preprocess.service;

import com.example.geo_preprocess.models.CoordinateParam;
import com.example.geo_preprocess.models.ResampleParam;
import com.example.geo_preprocess.models.TiffMetaData;

public interface GeoPreProService {

    /**
     * 获取tiff文件元数据
     *
     * @param imagePath 文件路径
     * @return 元数据
     */
    TiffMetaData getMetadata(String imagePath);

    /**
     * 更换坐标系
     *
     * @param param 坐标系参数
     * @return 输出后文件路径
     */
    String changeCoordination(CoordinateParam param);

    /**
     * 重采样
     *
     * @param param 重采样参数
     * @return 采样后文件路径
     */
    String resampleImage(ResampleParam param);

}
