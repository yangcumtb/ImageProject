package com.example.geo_preprocess.controller;

import com.example.geo_preprocess.models.CoordinateParam;
import com.example.geo_preprocess.models.ResampleParam;
import com.example.geo_preprocess.models.ResponseData;
import com.example.geo_preprocess.models.TiffMetaData;
import com.example.geo_preprocess.service.GeoPreProService;
import com.example.geo_preprocess.service.impl.GeoPreProServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/imagePreprocessing")
public class CommonController {

    @Autowired
    private GeoPreProService geoPreProService;

    /**
     * 获取影像元数据接口
     *
     * @param filePath 影像路径
     * @return 元数据
     */
    @GetMapping("/MetaData")
    public ResponseData getMetaData(String filePath) {
        try {
            TiffMetaData data = geoPreProService.getMetadata(filePath);
            return ResponseData.success(data);
        } catch (Exception e) {
            //如果文件无法读取，返回原因
            return ResponseData.error(e.getCause().toString());
        }
    }

    /**
     * 更换坐标系
     *
     * @param outPath    输出路径
     * @param targetEPSG 目标epsg
     * @return
     */
    @PostMapping("/Projecttion")
    public ResponseData changeCoor(
            @RequestParam("filePath") String filePath,
            @RequestParam("outPath") String outPath,
            @RequestParam("targetEPSG") String targetEPSG
    ) {

        CoordinateParam param = new CoordinateParam();
        param.setFilePath(filePath);
        param.setOutPath(outPath);
        param.setTargetEPSG(targetEPSG);
        Map<String, String> result = geoPreProService.changeCoordination(param);
        return ResponseData.success(result);
    }

    /**
     * 重采样
     *
     * @param filePath       文件路径
     * @param outPath        输出路径
     * @param resampleMethod 采样方法
     * @param reSizeX        采样后x像素值
     * @param reSizeY        采样后y像素值
     * @return
     */
    @PostMapping("/Resample")
    public ResponseData resample(
            @RequestParam("filePath") String filePath,
            @RequestParam("outPath") String outPath,
            @RequestParam("resampleMethod") String resampleMethod,
            @RequestParam("reSizeX") Integer reSizeX,
            @RequestParam("reSizeY") Integer reSizeY
    ) {
        ResampleParam resampleParam = new ResampleParam();
        resampleParam.setResampleMethod(resampleMethod);
        resampleParam.setFilePath(filePath);
        resampleParam.setOutPath(outPath);
        resampleParam.setReSizeX(reSizeX);
        resampleParam.setReSizeY(reSizeY);

        Map<String, String> result = geoPreProService.resampleImage(resampleParam);
        return ResponseData.success(result);
    }

    /**
     * 转换影像格式，需要支持的格式：
     *
     * @param filePath     文件路径
     * @param outputPath   输出路径
     * @param targetFormat 目标格式
     * @return
     */
    @PostMapping("/ChangeFormat")
    public ResponseData changeFormat(
            @RequestParam("filePath") String filePath,
            @RequestParam("outputPath") String outputPath,
            @RequestParam("targetFormat") String targetFormat
    ) throws IOException {
        File out = new File(outputPath);
        File input = new File(filePath);
        if (!out.exists()) {
            out.mkdir();
        }

        if (GeoPreProServiceImpl.getFileExtension(input).equals("gif")) {
            //对于gif文件，只能保存为png或者jpg
            String oupath = GeoPreProServiceImpl.gifChange(filePath, outputPath, targetFormat);
            if (oupath.equals("")) {
                return ResponseData.error("转换失败！");
            }
            Map<String, String> res = new HashMap<>();
            res.put("outputPath", oupath);
            return ResponseData.success(res);
        }
        geoPreProService.changeFormat(filePath, outputPath, targetFormat);
        return ResponseData.success();
    }
}
