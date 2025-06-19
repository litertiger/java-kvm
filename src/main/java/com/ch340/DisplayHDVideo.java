package com.ch340;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamResolution;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

public class DisplayHDVideo extends JFrame {
    private JPanel videoPanel;
    private JComboBox<String> resolutionCombo;
    private JButton startButton;
    private JButton stopButton;
    private Webcam webcam;
    private boolean isRunning = false;
    private javax.swing.Timer timer;


    public DisplayHDVideo() {
        setTitle("高清摄像头视频显示");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1280, 720);
        setLocationRelativeTo(null);

        // 创建视频显示面板
        videoPanel = new JPanel();
        videoPanel.setPreferredSize(new Dimension(1280, 720));
        videoPanel.setBackground(Color.BLACK);

        // 创建控制面板
        JPanel controlPanel = new JPanel();
        resolutionCombo = new JComboBox<>(new String[]{"720P", "1080P"});
        startButton = new JButton("开始");
        stopButton = new JButton("停止");
        stopButton.setEnabled(false);

        controlPanel.add(new JLabel("分辨率:"));
        controlPanel.add(resolutionCombo);
        controlPanel.add(startButton);
        controlPanel.add(stopButton);

        // 添加事件监听器
        startButton.addActionListener(e -> startVideo());
        stopButton.addActionListener(e -> stopVideo());
        resolutionCombo.addActionListener(e -> changeResolution());

        // 布局
        setLayout(new BorderLayout());
        add(videoPanel, BorderLayout.CENTER);
        add(controlPanel, BorderLayout.SOUTH);

        // 列出可用的摄像头
        List<Webcam> webcams = Webcam.getWebcams();
        if (webcams.isEmpty()) {
            JOptionPane.showMessageDialog(this, "未检测到摄像头设备");
        } else {
            System.out.println("检测到以下摄像头设备：");
            for (Webcam w : webcams) {
                System.out.println(w.getName());
            }
        }
    }

    private void startVideo() {
        if (!isRunning) {
            try {
                System.out.println("正在初始化摄像头...");
                webcam = Webcam.getDefault();

                if (webcam == null) {
                    throw new Exception("未找到可用的摄像头");
                }
                Dimension hd1080 = new Dimension(1920, 1080);
                webcam.setCustomViewSizes(new Dimension[] { hd1080 }); // 添加自定义分辨率
                webcam.setViewSize(hd1080); // 应用1080p分辨率

                // setResolution();
                System.out.println("正在启动摄像头...");
                webcam.open();
                System.out.println("摄像头启动成功");

                isRunning = true;
                startButton.setEnabled(false);
                stopButton.setEnabled(true);
                resolutionCombo.setEnabled(false);

                // 使用Timer定时更新视频帧
                timer = new javax.swing.Timer(33, e -> {
                    try {
                        BufferedImage image = webcam.getImage();
                        if (image != null) {
                            SwingUtilities.invokeLater(() -> {
                                Graphics g = videoPanel.getGraphics();
                                if (g != null) {
                                    g.drawImage(image, 0, 0, videoPanel.getWidth(), videoPanel.getHeight(), null);
                                }
                            });
                        }
                    } catch (Exception ex) {
                        System.out.println("获取视频帧时发生错误: " + ex.getMessage());
                        stopVideo();
                    }
                });
                timer.start();

            } catch (Exception e) {
                System.out.println("启动摄像头时发生错误: " + e.getMessage());
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "无法启动摄像头: " + e.getMessage());
            }
        }
    }

    private void stopVideo() {
        if (isRunning) {
            isRunning = false;
            if (timer != null) {
                timer.stop();
            }
            if (webcam != null) {
                webcam.close();
            }
            startButton.setEnabled(true);
            stopButton.setEnabled(false);
            resolutionCombo.setEnabled(true);
        }
    }

    private void changeResolution() {
        if (isRunning) {
            stopVideo();
            startVideo();
        }
    }

    private void setResolution() {
        if (webcam != null) {
            String selectedResolution = (String) resolutionCombo.getSelectedItem();
            System.out.println("设置分辨率: " + selectedResolution);
            if ("720P".equals(selectedResolution)) {
                webcam.setViewSize(new Dimension(1280, 720));
            } else if ("1080P".equals(selectedResolution)) {
                //webcam.setViewSize(new Dimension(1920, 1080));
                // Dimension hd = new Dimension(1920, 1080);
                // webcam.setCustomViewSizes(new Dimension[] { hd });
                Dimension hd1080 = new Dimension(1920, 1080);
                webcam.setCustomViewSizes(new Dimension[] { hd1080 }); // 添加自定义分辨率
                webcam.setViewSize(hd1080); // 应用1080p分辨率
            }
            System.out.println("分辨率设置完成: " + webcam.getViewSize());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new DisplayHDVideo().setVisible(true);
        });
    }
} 