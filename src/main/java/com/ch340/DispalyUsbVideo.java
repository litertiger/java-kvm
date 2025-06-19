package com.ch340;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamResolution;

public class DispalyUsbVideo extends JFrame {
    private JPanel videoPanel;
    private JComboBox<String> resolutionCombo;
    private JButton startButton;
    private JButton stopButton;
    private Webcam webcam;
    private boolean isRunning = false;

    public DispalyUsbVideo() {
        setTitle("USB摄像头视频显示");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);

        // 创建视频显示面板
        videoPanel = new JPanel();
        videoPanel.setPreferredSize(new Dimension(640, 480));
        videoPanel.setBackground(Color.BLACK);

        // 创建控制面板
        JPanel controlPanel = new JPanel();
        resolutionCombo = new JComboBox<>(new String[]{"320x240", "640x480"});
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
    }

    private void startVideo() {
        if (!isRunning) {
            try {
                webcam = Webcam.getDefault();
                setResolution();
                webcam.open();
                isRunning = true;
                startButton.setEnabled(false);
                stopButton.setEnabled(true);
                resolutionCombo.setEnabled(false);

                // 创建新线程处理视频
                new Thread(() -> {
                    try {
                        while (isRunning) {
                            BufferedImage image = webcam.getImage();
                            if (image != null) {
                                SwingUtilities.invokeLater(() -> {
                                    videoPanel.getGraphics().drawImage(image, 0, 0, videoPanel.getWidth(), videoPanel.getHeight(), null);
                                });
                            }
                            Thread.sleep(33); // 约30FPS
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).start();
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "无法启动摄像头: " + e.getMessage());
            }
        }
    }

    private void stopVideo() {
        if (isRunning) {
            isRunning = false;
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
            if ("320x240".equals(selectedResolution)) {
                webcam.setViewSize(WebcamResolution.QVGA.getSize());
            } else if ("640x480".equals(selectedResolution)) {
                webcam.setViewSize(WebcamResolution.VGA.getSize());
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new DispalyUsbVideo().setVisible(true);
        });
    }
}
