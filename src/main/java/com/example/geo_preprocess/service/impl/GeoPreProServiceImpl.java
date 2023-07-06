package com.example.geo_preprocess.service.impl;

import com.example.geo_preprocess.models.CoordinateParam;
import com.example.geo_preprocess.models.ResampleParam;
import com.example.geo_preprocess.models.TiffMetaData;
import com.example.geo_preprocess.service.GeoPreProService;
import com.example.geo_preprocess.tools.ExeExecution;
import com.example.geo_preprocess.tools.FormatEum;
import org.gdal.gdal.Band;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconst;
import org.gdal.osr.SpatialReference;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;

@Service
public class GeoPreProServiceImpl implements GeoPreProService {

    static {
        gdal.AllRegister();
    }

    /**
     * 获取tiff文件元数据
     *
     * @param filePath 文件路径
     * @return 元数据
     */
    @Override
    public TiffMetaData getMetadata(String filePath) {
//        System.setProperty("PROJ_LIB", "D:\\Program Files\\Java\\release-1930-x64-gdal-3-6-3-mapserver-8-0-0\\bin\\proj7\\share");
//        File file = new File(imagePath);
//        System.out.println("PROJ_LIB: " + System.getenv("PROJ_LIB"));

        File file = new File(filePath);
        // 获取文件的修改时间
        long modifiedTime = file.lastModified();
        TiffMetaData tiffMetaData = new TiffMetaData();
        // 获取文件后缀
        tiffMetaData.setFileSuffix(getFileExtension(file));
        // 获取文件大小（以KB为单位）
        tiffMetaData.setFileSize(getFileSize(file));
        // 获取文件路径
        tiffMetaData.setImagePath(filePath);
        // 将修改时间转换为日期对象
        tiffMetaData.setModificationTime(new Date(modifiedTime));
        // 获取名称
        tiffMetaData.setImageName(file.getName());
        Dataset imageSet = gdal.Open(filePath, gdalconst.GA_ReadOnly);
        //获取空间仿射变换参数
        tiffMetaData.setAffineTransformation(imageSet.GetGeoTransform());


        //获取图像的压缩方式
        tiffMetaData.setCompressMode(imageSet.GetMetadataItem("COMPRESSION"));

        //获取色彩模式
        tiffMetaData.setColorPattern(getColorPatten(imageSet));

        //获取宽度和高度
        tiffMetaData.setImageHeight(imageSet.getRasterYSize());
        tiffMetaData.setImageWidth(imageSet.getRasterXSize());

        // 获取坐标系
        SpatialReference spatialRef = new SpatialReference(imageSet.GetProjection());
        // 获取EPSG代码
        String epsgCode = spatialRef.GetAttrValue("AUTHORITY", 1);
        //获取epsg代码
        tiffMetaData.setEpsg("EPSG:" + epsgCode);
        //获取元数据坐标系
        switch (epsgCode) {
            case "4326":
                tiffMetaData.setProjection("WGS_1984(WGS84坐标系)");
                break;
            case "3857":
                tiffMetaData.setProjection("WGS_1984(Web墨卡托投影)");
                break;
            case "4490":
                tiffMetaData.setProjection("China_2000");
                break;
        }

        return tiffMetaData;
    }


    /**
     * 更换坐标系
     *
     * @param param 坐标系参数
     * @return 输出后文件路径
     */
    @Override
    public Map<String, String> changeCoordination(CoordinateParam param) {

//        GDAL代码，存在与postgis冲突的环境变量
//         打开源图像数据集
        Dataset sourceDataset = gdal.Open(param.getFilePath(), gdalconst.GA_ReadOnly);
        SpatialReference spatialRef = new SpatialReference(sourceDataset.GetProjection());
        // 获取坐标系名称
        String coordinateSystem = spatialRef.GetAttrValue("DATUM");
        // 获取EPSG代码
        String sourceEPSG = "EPSG:" + spatialRef.GetAttrValue("AUTHORITY", 1);

        //实例化exe文件的执行对象
        ExeExecution.doChangeCoordinate(param.getFilePath(), param.getOutPath(), sourceEPSG, param.getTargetEPSG());

        Map<String, String> result = new HashMap<>();
        result.put("sourceCoordinateSystem", coordinateSystem);
        if (param.getTargetEPSG().equals("EPSG:4490")) {
            result.put("newCoordinateSystem", "China_2000");
        } else if (param.getTargetEPSG().equals("EPSG:3857")) {
            result.put("newCoordinateSystem", "WGS_1984(Web墨卡托投影)");
        } else if (param.getTargetEPSG().equals("EPSG:4326")) {
            result.put("newCoordinateSystem", "WGS_1984(WGS84坐标系)");
        }
        result.put("ouputPath", param.getOutPath());

        return result;

    }

    /**
     * 重采样
     *
     * @param param 重采样参数
     * @return 采样后文件路径
     */
    @Override
    public Map<String, String> resampleImage(ResampleParam param) {

        //实例化exe文件的执行对象
        ExeExecution.doResampleOperation(param.getFilePath(), param.getOutPath(), param.getReSizeX(), param.getReSizeY(), param.getResampleMethod());
        //设置返回值参数
        Map<String, String> result = new HashMap<>();
        result.put("newWidth", String.valueOf(param.getReSizeX()));
        result.put("newHeight", String.valueOf(param.getReSizeY()));
        result.put("outputPath", param.getOutPath());

        return result;
    }

    /**
     * 格式转换方法
     *
     * @param filePath     文件路径
     * @param targetFormat 目标格式
     * @return 返回结果
     */
    @Override
    public Map<String, String> changeFormat(String filePath, String outputPath, String targetFormat) {

        Dataset imageSet = gdal.Open(filePath);
        File file = new File(filePath);
        String ouptPath = ExeExecution.doChangeFormat(filePath, outputPath, file.getName(), targetFormat, FormatEum.getSuffixValue(targetFormat), imageSet.getRasterCount());
        imageSet.delete();
        Map<String, String> res = new HashMap<>();
        res.put("outputPath", ouptPath);
        return res;
    }


    /**
     * 获取文件后缀
     *
     * @param file 文件
     * @return 后缀
     */
    public static String getFileExtension(File file) {
        String fileName = file.getName();
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < fileName.length() - 1) {
            return fileName.substring(dotIndex + 1).toLowerCase();
        }
        return "";
    }

    /**
     * 获取文件大小
     *
     * @param file 文件
     * @return 大小
     */
    private static long getFileSize(File file) {
        return file.length() / 1024; // 文件大小以KB为单位
    }

    /**
     * 获取色彩模式
     *
     * @param dataset 数据集
     * @return 结果
     */
    private static String getColorPatten(Dataset dataset) {

        StringBuilder pattn = new StringBuilder();

        if (dataset != null) {
            int bandSize = dataset.getRasterCount();

            for (int i = 1; i <= bandSize; i++) {
                Band band = dataset.GetRasterBand(i);

                String bandName = "波段" + i + ":";

                //获取色彩模式
                int color = band.GetColorInterpretation();

                if (color == gdalconst.GCI_GrayIndex) {
                    pattn.append(bandName).append("灰度图像").append("\n");
                } else if (color == gdalconst.GCI_PaletteIndex) {
                    pattn.append(bandName).append("调色板索引图像").append("\n");

                } else if (color == gdalconst.GCI_RedBand) {
                    pattn.append(bandName).append("红色波段图像").append("\n");

                } else if (color == gdalconst.GCI_GreenBand) {
                    pattn.append(bandName).append("绿色波段图像").append("\n");

                } else if (color == gdalconst.GCI_BlueBand) {
                    pattn.append(bandName).append("蓝色波段图像").append("\n");

                } else if (color == gdalconst.GCI_AlphaBand) {
                    pattn.append(bandName).append("透明通道图像").append("\n");
                }
            }
        }

        return pattn.toString();
    }


    /**
     * gif图片转换要求
     *
     * @param targetFormat 目标格式
     */
    public static String gifChange(String inputpath, String outputFilePath, String targetFormat) throws IOException {
        // 加载 GIF 图像
        File gifFile = new File(inputpath);
        ImageInputStream input = ImageIO.createImageInputStream(gifFile);

        // 获取 GIF 图像的 ImageReader
        Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
        ImageReader reader = null;
        while (readers.hasNext()) {
            reader = readers.next();
            if (reader.getFormatName().equalsIgnoreCase("gif")) {
                break;
            }
        }
        // 读取 GIF 图像的每一帧并保存为 PNG 图像
        if (reader != null) {
            reader.setInput(input);
        }
        int numFrames = 0;
        if (reader != null) {
            numFrames = reader.getNumImages(true);
        }
        for (int frameIndex = 0; frameIndex < numFrames; frameIndex++) {
            // 获取当前帧的图像数据
            BufferedImage frame = reader.read(frameIndex);

            if (targetFormat.equals("PNG")) {
                File outputFile = new File(outputFilePath + "frame_" + frameIndex + ".png");
                ImageIO.write(frame, "png", outputFile);
            } else {
                File outputFile = new File(outputFilePath + "frame_" + frameIndex + ".jpg");
                ImageIO.write(frame, "jpeg", outputFile);
            }
        }

        // 关闭 ImageInputStream 和 ImageReader
        if (reader != null) {
            reader.dispose();
        }
        input.close();
        return "";
    }
}
