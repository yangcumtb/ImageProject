package com.example.geo_preprocess.controller;

import com.example.geo_preprocess.models.CoordinateParam;
import com.example.geo_preprocess.models.ResampleParam;
import com.example.geo_preprocess.models.ResponseData;
import com.example.geo_preprocess.models.TiffMetaData;
import com.example.geo_preprocess.service.GeoPreProService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Map;

@RestController
@RequestMapping("/")
public class CommonController {

    @Resource
    private GeoPreProService geoPreProService;

    /**
     * 获取影像元数据接口
     *
     * @param imagePath 影像路径
     * @return 元数据
     */
    @GetMapping("/MetaData")
    public ResponseData getMetaData(String imagePath) {
        TiffMetaData data = geoPreProService.getMetadata(imagePath);
        return ResponseData.success(data);
    }

    /**
     * 更换坐标系
     *
     * @param coordinateParam 坐标系参数
     * @return 文件路径
     */
    @PostMapping("/Projecttion")
    public ResponseData changeCoor(@RequestBody CoordinateParam coordinateParam) {
        Map<String, String> result = geoPreProService.changeCoordination(coordinateParam);
        return ResponseData.success(result);
    }

    /**
     * 重采样
     *
     * @param resampleParam 重采样参数
     * @return 文件路径
     */
    @PostMapping("/Resample")
    public ResponseData resample(@RequestBody ResampleParam resampleParam) {
        Map<String, Integer> result = geoPreProService.resampleImage(resampleParam);
        return ResponseData.success(result);
    }

}
