package com.ch340;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamResolution;
import java.awt.Dimension;
import java.util.List;

public class TestCam {
    public static void main(String[] args) {
        Webcam webcam = Webcam.getDefault();
        if (webcam == null) {
            System.out.println("未检测到摄像头设备");
            return;
        }

        System.out.println("摄像头名称: " + webcam.getName());
        
        // 获取摄像头支持的所有分辨率
        Dimension[] sizes = webcam.getViewSizes();
        System.out.println("\n摄像头支持的分辨率列表：");
        for (Dimension size : sizes) {
            System.out.println(size.getWidth() + "x" + size.getHeight());
        }

        // 尝试设置1080p（1920x1080）分辨率
        Dimension hd = new Dimension(1920, 1080);
        webcam.setCustomViewSizes(new Dimension[] { hd });
        webcam.setViewSize(hd);

        webcam.open();
        System.out.println("\n当前设置的分辨率: " + webcam.getViewSize());
        
        // 获取实际捕获的图像尺寸
        if (webcam.isOpen()) {
            System.out.println("实际捕获的图像尺寸: " + webcam.getImage().getWidth() + "x" + webcam.getImage().getHeight());
        }
        
        webcam.close();
    }
}