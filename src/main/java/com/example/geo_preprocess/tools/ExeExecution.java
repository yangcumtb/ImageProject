package com.example.geo_preprocess.tools;

import org.springframework.scheduling.annotation.Async;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Objects;

public class ExeExecution {

    /**
     * @param inputFile  输入文件
     * @param outputFile 输出文件
     * @param width      采样宽度
     * @param height     采样高度
     * @param method     采样方法
     */
    @Async
    public void doResampleOperation(String inputFile, String outputFile, int width, int height, String method) {
        try {
            // 设置.exe文件路径
            String exePath = "D:\\IdeaProjects\\geo_preprocess\\src\\main\\resources\\lib\\gdalwarp.exe";

            // 设置命令参数
            String[] command = {
                    exePath,
                    "-r",
                    method,
                    "-ts",
                    String.valueOf(width),
                    String.valueOf(height),
                    inputFile,
                    outputFile
            };

            // 创建进程并执行命令
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            Process process = processBuilder.start();

            // 获取命令的输入流和输出流
            InputStream inputStream = process.getInputStream();
            InputStream errorStream = process.getErrorStream();

            // 读取命令的输出
            String output = readStream(inputStream);

            // 读取命令的错误输出
            String errorOutput = readStream(errorStream);
            // 输出命令的输出和错误输出
            System.out.println("命令输出:\n" + output);
            System.out.println("错误输出:\n" + errorOutput);
            // 等待命令执行完成
            int exitCode = process.waitFor();
            System.out.println("退出码: " + exitCode);

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }


    /**
     * 更换坐标系
     *
     * @param inputPath  源文件
     * @param outputPath 输出文件
     * @param sourceEPSG 源坐标系
     * @param targetEPSG 目标坐标系
     */
    public void doChangeCoordinate(String inputPath, String outputPath, String sourceEPSG, String targetEPSG) {
        try {
            // 设置.exe文件路径
            String exePath = "D:\\IdeaProjects\\geo_preprocess\\src\\main\\resources\\lib\\gdalwarp.exe";

            // 设置命令参数
            String[] command = {
                    exePath,
                    "-s_srs",
                    sourceEPSG,
                    "-t_srs",
                    targetEPSG,
                    "\"" + inputPath + "\"",
                    "\"" + outputPath + "\"",
            };
            // 设置环境变量

            ProcessBuilder processBuilder = new ProcessBuilder(command);
            Map<String, String> env = processBuilder.environment();
            env.put("PROJ_LIB", "D:\\Program Files\\PostgreSQL\\13\\share\\contrib\\postgis-3.1\\proj");
            // 创建进程并执行命令

            Process process = processBuilder.start();

            // 获取命令的输入流和输出流
            InputStream inputStream = process.getInputStream();
            InputStream errorStream = process.getErrorStream();

            // 读取命令的输出
            String output = readStream(inputStream);

            // 读取命令的错误输出
            String errorOutput = readStream(errorStream);
            // 输出命令的输出和错误输出
            System.out.println("命令输出:\n" + output);
            System.out.println("错误输出:\n" + errorOutput);
            // 等待命令执行完成
            int exitCode = process.waitFor();
            System.out.println("退出码: " + exitCode);

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }


    // 读取流并返回字符串
    private static String readStream(InputStream inputStream) throws IOException {
        StringBuilder output = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        while ((line = reader.readLine()) != null) {
            output.append(line).append("\n");
        }
        return output.toString();
    }
}
