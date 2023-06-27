package com.example.geo_preprocess.service.impl;

import com.example.geo_preprocess.models.CoordinateParam;
import com.example.geo_preprocess.models.ResampleParam;
import com.example.geo_preprocess.models.TiffMetaData;
import com.example.geo_preprocess.service.GeoPreProService;
import com.example.geo_preprocess.tools.ExeExecution;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.gdal;
import org.gdal.osr.SpatialReference;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.processing.Operations;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.gce.geotiff.GeoTiffWriter;
import org.geotools.referencing.CRS;
import org.geotools.util.factory.Hints;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.springframework.stereotype.Service;

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
     * @param imagePath 文件路径
     * @return 元数据
     */
    @Override
    public TiffMetaData getMetadata(String imagePath) {
//        System.setProperty("PROJ_LIB", "D:\\Program Files\\Java\\release-1930-x64-gdal-3-6-3-mapserver-8-0-0\\bin\\proj7\\share");
//        File file = new File(imagePath);
//        System.out.println("PROJ_LIB: " + System.getenv("PROJ_LIB"));

        File file = new File(imagePath);
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
        Dataset imageSet = gdal.Open(imagePath);
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
    public Map<String, String> changeCoordination(CoordinateParam param) {

        //GDAL代码，存在与postgis冲突的环境变量
        // 打开源图像数据集
//        Dataset sourceDataset = gdal.Open(param.getFilePath(), gdalconst.GA_ReadOnly);
//        SpatialReference spatialRef = new SpatialReference(sourceDataset.GetProjection());
//        // 获取坐标系名称
//        String coordinateSystem = spatialRef.GetAttrValue("DATUM");
//        // 获取EPSG代码
//        String sourceEPSG = "EPSG:" + spatialRef.GetAttrValue("AUTHORITY", 1);
//
//        //实例化exe文件的执行对象
//        ExeExecution exeExecution = new ExeExecution();
//        exeExecution.doChangeCoordinate(param.getFilePath(), param.getOutPath(), sourceEPSG, param.getTargetEPSG());
//
//        File file = new File(param.getFilePath());
        File file = new File(param.getFilePath());
        try {
            GeoTiffReader reader = new GeoTiffReader(file, new Hints(Hints.FORCE_LONGITUDE_FIRST_AXIS_ORDER, Boolean.TRUE));
            GridCoverage2D coverage2D = reader.read(null);
            //获取当前坐标系
            CoordinateReferenceSystem crs = coverage2D.getCoordinateReferenceSystem2D();
            System.out.println(String.format("原坐标系：%s", crs.getName()));
            final CoordinateReferenceSystem WGS = CRS.decode("EPSG:3857");
            //执行重投影
            GridCoverage2D new2D = (GridCoverage2D) Operations.DEFAULT.resample(coverage2D, WGS);

            //获取重投影后的坐标信息
            CoordinateReferenceSystem newCrs = new2D.getCoordinateReferenceSystem2D();
            System.out.println(String.format("新坐标系：%s", newCrs.getName()));
            // 指定保存路径和文件名
            File outputFile = new File(file.getParent() + "\\" + WGS.getName().toString() + ".tif");

            // 获取 GridCoverageWriter
            GeoTiffWriter writer = new GeoTiffWriter(outputFile);

            // 设置输出的坐标参考系统为重投影后的坐标参考系统
            writer.write(new2D, null);
            System.out.println("重投影数据已保存为 GeoTIFF 文件：" + outputFile.getAbsolutePath());

            // 关闭写入器
            writer.dispose();
            Map<String, String> result = new HashMap<>();
            result.put("sourceCoordinateSystem", crs.getName().toString());
            result.put("newCoordinateSystem", WGS.getName().toString());

            return result;
        } catch (IOException | FactoryException e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * 重采样
     *
     * @param param 重采样参数
     * @return 采样后文件路径
     */
    @Override
    public Map<String, Integer> resampleImage(ResampleParam param) {

        //实例化exe文件的执行对象
        ExeExecution exeExecution = new ExeExecution();
        exeExecution.doResampleOperation(param.getFilePath(), param.getOutPath(), param.getReSizeX(), param.getReSizeY(), param.getResampleMethod());
        //设置返回值参数
        Map<String, Integer> result = new HashMap<>();
        result.put("newWidth", param.getReSizeX());
        result.put("newHeight", param.getReSizeY());

        return result;
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
