package com.example.geo_preprocess.service.impl;

import com.example.geo_preprocess.models.CoordinateParam;
import com.example.geo_preprocess.models.ResampleParam;
import com.example.geo_preprocess.models.TiffMetaData;
import com.example.geo_preprocess.service.GeoPreProService;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconst;
import org.gdal.osr.SpatialReference;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Date;
import java.util.Hashtable;

@Service
public class GeoPreProServiceImpl implements GeoPreProService {

    static {
        gdal.AllRegister();
    }

    /**
     * 获取tiff文件元数据
     *
     * @param imagePath 文件路径
     * @return 元数据
     */
    @Override
    public TiffMetaData getMetadata(String imagePath) {
//        System.setProperty("PROJ_LIB", "D:\\Program Files\\Java\\release-1930-x64-gdal-3-6-3-mapserver-8-0-0\\bin\\proj7\\share");
//        File file = new File(imagePath);
        System.out.println("PROJ_LIB: " + System.getenv("PROJ_LIB"));


        File file = new File("E:\\data\\tiff_test\\chengnanjiedao0.tif");
        // 获取文件的修改时间
        long modifiedTime = file.lastModified();
        TiffMetaData tiffMetaData = new TiffMetaData();
        // 获取文件后缀
        tiffMetaData.setFileSuffix(getFileExtension(file));
        // 获取文件大小（以KB为单位）
        tiffMetaData.setFileSize(getFileSize(file));
        // 获取文件路径
        tiffMetaData.setImagePath(imagePath);
        // 将修改时间转换为日期对象
        tiffMetaData.setModificationTime(new Date(modifiedTime));
        // 获取名称
        tiffMetaData.setImageName(file.getName());
        Dataset imageSet = gdal.Open("E:\\data\\tiff_test\\chengnanjiedao0.tif");
        //获取空间仿射变换参数
        tiffMetaData.setAffineTransformation(imageSet.GetGeoTransform());


        //获取图像的压缩方式
        tiffMetaData.setCompressMode(imageSet.GetMetadataItem("COMPRESSION"));

        Hashtable met = imageSet.GetMetadata_Dict();
        //获取色彩模式
        tiffMetaData.setColorPattern(getColorPatten(imageSet));

        //获取宽度和高度
        tiffMetaData.setImageHeight(imageSet.getRasterYSize());
        tiffMetaData.setImageWidth(imageSet.getRasterXSize());

        // 获取坐标系
        SpatialReference spatialRef = new SpatialReference(imageSet.GetProjection());
        // 获取坐标系名称
        String coordinateSystem = spatialRef.GetAttrValue("DATUM");
        // 获取EPSG代码
        String epsgCode = spatialRef.GetAttrValue("AUTHORITY", 1);
        //获取epsg代码
        tiffMetaData.setEpsg(epsgCode);
        //获取元数据坐标系
        tiffMetaData.setProjection(coordinateSystem);


        return tiffMetaData;
    }


    /**
     * 更换坐标系
     *
     * @param param 坐标系参数
     * @return 输出后文件路径
     */
    @Override
    public String changeCoordination(CoordinateParam param) {
        return null;
    }

    /**
     * 重采样
     *
     * @param param 重采样参数
     * @return 采样后文件路径
     */
    @Override
    public String resampleImage(ResampleParam param) {
        return null;
    }


    /**
     * 获取文件后缀
     *
     * @param file 文件
     * @return 后缀
     */
    private static String getFileExtension(File file) {
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
        return null;
    }

}
