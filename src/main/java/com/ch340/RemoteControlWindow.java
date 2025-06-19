package com.ch340;

import com.github.sarxos.webcam.Webcam;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * 远程控制窗口类
 * 实现USB摄像头视频显示、鼠标事件捕获和键盘事件捕获功能
 */
public class RemoteControlWindow extends JFrame {
    // 视频显示面板
    private JPanel videoPanel;
    // 摄像头对象
    private Webcam webcam;
    // 视频运行状态标志
    private boolean isRunning = false;
    // 视频更新定时器
    private javax.swing.Timer timer;
    // 记录鼠标最后位置
    private Point lastMousePosition;
    // 事件日志字符串构建器
    private StringBuilder eventLog;
    // 日志显示区域
    private JTextArea logArea;
    // 摄像头选择下拉框
    private JComboBox<String> webcamList;
    // 上次鼠标移动时间
    private long lastMouseMoveTime = 0;
    // 鼠标移动时间阈值（毫秒）
    private static final long MOUSE_MOVE_THRESHOLD = 50;
    // 目标分辨率宽度
    private static final int TARGET_WIDTH = 1920;
    // 目标分辨率高度
    private static final int TARGET_HEIGHT = 1080;
    // 日志面板
    private JPanel logPanel;
    // 日志显示状态
    private boolean isLogVisible = true;
    private boolean isFullScreen = false;
    private JPanel fullScreenControls;
    private JPanel controlPanel;
    // private JPanel keyboardPanel;
    private GraphicsDevice device;
    private CH9329Serial serial = new CH9329Serial();
    public MouseCommander mouse = new MouseCommander(serial);
    public  KeyboardCommander key = new KeyboardCommander(serial);

    public RemoteControlWindow() {
        setTitle("远程控制窗口");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1920, 1080);
        setLocationRelativeTo(null);
        
        // 获取默认显示设备
        device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();

        // 创建主面板
        JPanel mainPanel = new JPanel(new BorderLayout());

        // 创建控制面板
        controlPanel = new JPanel();
        webcamList = new JComboBox<>();
        JButton showButton = new JButton("显示视频");
        JButton toggleLogButton = new JButton("隐藏日志");
        JButton fullScreenButton = new JButton("全屏显示");
        controlPanel.add(new JLabel("选择摄像头:"));
        controlPanel.add(webcamList);
        controlPanel.add(showButton);
        controlPanel.add(toggleLogButton);
        controlPanel.add(fullScreenButton);

        // 创建全屏控制面板
        fullScreenControls = new JPanel();
        fullScreenControls.setLayout(new FlowLayout(FlowLayout.CENTER));
        fullScreenControls.setOpaque(false);
        JButton exitFullScreenButton = new JButton("退出全屏");
        fullScreenControls.add(exitFullScreenButton);
        fullScreenControls.setVisible(false);

        // 添加全屏按钮事件监听器
        fullScreenButton.addActionListener(e -> enterFullScreen());
        exitFullScreenButton.addActionListener(e -> exitFullScreen());

        // 创建视频显示面板
        videoPanel = new JPanel() {
            private BufferedImage buffer;

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (webcam != null && webcam.isOpen()) {
                    BufferedImage image = webcam.getImage();
                    if (image != null) {
                        // 创建双缓冲
                        if (buffer == null || buffer.getWidth() != getWidth() || buffer.getHeight() != getHeight()) {
                            buffer = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
                        }
                        Graphics2D g2d = buffer.createGraphics();
                        g2d.drawImage(image, 0, 0, getWidth(), getHeight(), null);
                        g2d.dispose();
                        
                        // 绘制缓冲图像
                        g.drawImage(buffer, 0, 0, null);
                    }
                }
            }
        };
        videoPanel.setLayout(null); // 使用绝对布局
        videoPanel.setPreferredSize(new Dimension(1280, 720));
        videoPanel.setBackground(Color.BLACK);
        videoPanel.setFocusable(false); // 视频面板不需要焦点

    

        // 添加全屏控制面板到视频面板
        videoPanel.add(fullScreenControls);
        fullScreenControls.setBounds(0, 0, 200, 40);

        // 将键盘面板添加到视频面板
        // videoPanel.add(keyboardPanel);

        // 添加鼠标移动监听器到视频面板
        videoPanel.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                if (isFullScreen) {
                    // 当鼠标移动到顶部时显示控制栏
                    if (e.getY() < 50) {
                        fullScreenControls.setVisible(true);
                    } else {
                        fullScreenControls.setVisible(false);
                    }
                }
            }
        });

        // 创建日志面板
        logPanel = new JPanel(new BorderLayout());
        logArea = new JTextArea();
        logArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setPreferredSize(new Dimension(300, 0));
        
        // 添加隐藏/显示日志按钮事件监听器
        toggleLogButton.addActionListener(e -> {
            isLogVisible = !isLogVisible;
            toggleLogButton.setText(isLogVisible ? "隐藏日志" : "显示日志");
            logPanel.setVisible(isLogVisible);
            
            // 调整视频面板大小
            if (isLogVisible) {
                videoPanel.setPreferredSize(new Dimension(1280, 720));
            } else {
                videoPanel.setPreferredSize(new Dimension(1580, 720)); // 增加宽度以利用释放的空间
            }
            
            mainPanel.revalidate();
            mainPanel.repaint();
        });
        
        logPanel.add(scrollPane, BorderLayout.CENTER);

        // 添加组件到主面板
        mainPanel.add(controlPanel, BorderLayout.NORTH);
        mainPanel.add(videoPanel, BorderLayout.CENTER);
        mainPanel.add(logPanel, BorderLayout.EAST);

        // 设置内容面板
        setContentPane(mainPanel);

        // 初始化事件日志
        eventLog = new StringBuilder();

        // 初始化摄像头列表
        updateWebcamList();

        // 添加显示按钮事件监听器
        showButton.addActionListener(e -> {
            if (webcamList.getSelectedItem() != null) {
                stopVideo();
                startVideo();
            }
        });

        // 添加鼠标事件监听器
        videoPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                Point convertedPoint = convertToTargetCoordinates(e.getPoint());
                String buttonName = getMouseButtonName(e.getButton());
                logEvent(String.format("鼠标按下: %s", buttonName), convertedPoint.x, convertedPoint.y);
                try {
                    mouse.AbsClickMouse(buttonName, convertedPoint.x, convertedPoint.y);
                } catch (Exception ex) {
                    logEvent("鼠标按下失败: " + ex.getMessage());
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                Point convertedPoint = convertToTargetCoordinates(e.getPoint());
                String buttonName = getMouseButtonName(e.getButton());
                logEvent(String.format("鼠标释放: %s", buttonName), convertedPoint.x, convertedPoint.y);
                try {
                    mouse.AbsReleaseMouse(buttonName, convertedPoint.x, convertedPoint.y);
                } catch (Exception ex) {
                    logEvent("鼠标释放失败: " + ex.getMessage());
                }
            }

            @Override
            public void mouseClicked(MouseEvent e) {//鼠标点击事件,只有在同一个组件上press和 release 才会出发clicked事件，所以不需要重复发送这个指令
                Point convertedPoint = convertToTargetCoordinates(e.getPoint());
                String buttonName = getMouseButtonName(e.getButton());
                logEvent(String.format("忽略--》鼠标点击: %s", buttonName), convertedPoint.x, convertedPoint.y);
                 
                // try {
                //     mouse.AbsClickMouse(buttonName, convertedPoint.x, convertedPoint.y);
                //     Thread.sleep(100);
                //     mouse.AbsReleaseMouse(buttonName, convertedPoint.x, convertedPoint.y);
                // } catch (Exception ex) {
                //     logEvent("鼠标点击失败: " + ex.getMessage());
                // }
            }
        });

        videoPanel.addMouseWheelListener(new MouseWheelListener() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                Point convertedPoint = convertToTargetCoordinates(e.getPoint());
                int notches = e.getWheelRotation();
                String direction = notches < 0 ? "向上" : "向下";
                logEvent(String.format("鼠标滚轮: %s滚动 %d 单位", direction, Math.abs(notches)), 
                    convertedPoint.x, convertedPoint.y);
            }
        });

        videoPanel.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastMouseMoveTime < MOUSE_MOVE_THRESHOLD) {
                    return; // 忽略过快的鼠标移动
                }
                lastMouseMoveTime = currentTime;
                
                lastMousePosition = e.getPoint();
                Point convertedPoint = convertToTargetCoordinates(e.getPoint());
                logEvent("鼠标移动", convertedPoint.x, convertedPoint.y);
                try {
                    mouse.AbsMoveMouse(convertedPoint.x, convertedPoint.y);
                } catch (Exception ex) {
                    logEvent("鼠标移动失败: " + ex.getMessage());
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastMouseMoveTime < MOUSE_MOVE_THRESHOLD) {
                    return; // 忽略过快的鼠标拖动
                }
                lastMouseMoveTime = currentTime;
                
                lastMousePosition = e.getPoint();
                Point convertedPoint = convertToTargetCoordinates(e.getPoint());
                String buttonName = getMouseButtonName(e.getButton());
                logEvent(String.format("鼠标拖动: %s", buttonName), convertedPoint.x, convertedPoint.y);
            }
        });

        // 设置窗口可获取焦点
        setFocusable(true);
        requestFocusInWindow();

        // 添加窗口事件监听器
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowActivated(WindowEvent e) {
                requestFocusInWindow();
            }

            @Override
            public void windowClosing(WindowEvent e) {
                stopVideo();
            }
        });

        // 设置根面板
        JRootPane rootPane = getRootPane();
        rootPane.setFocusable(true);
        rootPane.requestFocusInWindow();

        // 添加焦点监听器到根面板
        rootPane.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                if (isVisible()) {
                    rootPane.requestFocusInWindow();
                }
            }
        });

        // 添加鼠标事件监听器到根面板
        rootPane.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                rootPane.requestFocusInWindow();
            }
        });

        // 使用AWTEventListener来监听全局键盘事件
        Toolkit.getDefaultToolkit().addAWTEventListener(new AWTEventListener() {
            @Override
            public void eventDispatched(AWTEvent event) {
                if (event instanceof KeyEvent) {
                    KeyEvent keyEvent = (KeyEvent) event;
                    if (keyEvent.getID() == KeyEvent.KEY_PRESSED) {
                        int keyCode = keyEvent.getKeyCode();
                        String keyName = getKeyName(keyCode);
                        logEvent(String.format("按键按下: %s (代码: %d)", keyName, keyCode));
                        try {
                            key.pressKey(keyName);
                        } catch (Exception ex) {
                            logEvent("按键按下失败: " + ex.getMessage());
                        }
                    }else if (keyEvent.getID() == KeyEvent.KEY_RELEASED) {
                        int keyCode = keyEvent.getKeyCode();
                        String keyName = getKeyName(keyCode);
                        logEvent(String.format("按键释放: %s (代码: %d)", keyName, keyCode));
                        try {
                            key.releaseKey(keyName);
                        } catch (Exception ex) {
                            logEvent("按键释放失败: " + ex.getMessage());
                        }
                    }
                }
            }
        }, AWTEvent.KEY_EVENT_MASK);
    }

    /**
     * 获取按键名称
     */
    private String getKeyName(int keyCode) {
        switch (keyCode) {
            case KeyEvent.VK_SHIFT: return "Shift";
            case KeyEvent.VK_CONTROL: return "Ctrl";
            case KeyEvent.VK_ALT: return "Alt";
            case KeyEvent.VK_F1: return "F1";
            case KeyEvent.VK_F2: return "F2";
            case KeyEvent.VK_F3: return "F3";
            case KeyEvent.VK_F4: return "F4";
            case KeyEvent.VK_F5: return "F5";
            case KeyEvent.VK_F6: return "F6";
            case KeyEvent.VK_F7: return "F7";
            case KeyEvent.VK_F8: return "F8";
            case KeyEvent.VK_F9: return "F9";
            case KeyEvent.VK_F10: return "F10";
            case KeyEvent.VK_F11: return "F11";
            case KeyEvent.VK_F12: return "F12";
            case KeyEvent.VK_ESCAPE: return "Esc";
            case KeyEvent.VK_TAB: return "Tab";
            case KeyEvent.VK_CAPS_LOCK: return "CapsLock";
            case KeyEvent.VK_ENTER: return "Enter";
            case KeyEvent.VK_BACK_SPACE: return "Backspace";
            case KeyEvent.VK_SPACE: return "Space";
            case KeyEvent.VK_INSERT: return "Insert";
            case KeyEvent.VK_DELETE: return "Delete";
            case KeyEvent.VK_HOME: return "Home";
            case KeyEvent.VK_END: return "End";
            case KeyEvent.VK_PAGE_UP: return "PageUp";
            case KeyEvent.VK_PAGE_DOWN: return "PageDown";
            case KeyEvent.VK_UP: return "Up";
            case KeyEvent.VK_DOWN: return "Down";
            case KeyEvent.VK_LEFT: return "←";
            case KeyEvent.VK_RIGHT: return "→";
            default:
                char keyChar = (char)keyCode;
                if (keyChar >= 'a' && keyChar <= 'z') {
                    return String.valueOf(keyChar).toUpperCase();
                }
                return String.valueOf(keyChar);
        }
    }

    /**
     * 更新摄像头列表
     */
    private void updateWebcamList() {
        webcamList.removeAllItems();
        List<Webcam> webcams = Webcam.getWebcams();
        for (Webcam w : webcams) {
            webcamList.addItem(w.getName());
        }
    }

    /**
     * 将原始坐标转换为目标分辨率坐标
     * @param originalPoint 原始坐标点
     * @return 转换后的坐标点
     */
    private Point convertToTargetCoordinates(Point originalPoint) {
        double scaleX = (double) TARGET_WIDTH / videoPanel.getWidth();
        double scaleY = (double) TARGET_HEIGHT / videoPanel.getHeight();
        
        int targetX = (int) (originalPoint.x * scaleX);
        int targetY = (int) (originalPoint.y * scaleY);
        
        return new Point(targetX, targetY);
    }

    /**
     * 启动视频显示
     */
    private void startVideo() {
        if (!isRunning) {
            try {
                List<Webcam> webcams = Webcam.getWebcams();
                String selectedWebcam = (String) webcamList.getSelectedItem();
                
                for (Webcam w : webcams) {
                    if (w.getName().equals(selectedWebcam)) {
                        webcam = w;
                        break;
                    }
                }

                if (webcam == null) {
                    throw new Exception("未找到选中的摄像头");
                }

                // 设置1080P分辨率
                // webcam.setViewSize(new Dimension(1920, 1080));
                // webcam.open();

                Dimension hd1080 = new Dimension(1920, 1080);
                webcam.setCustomViewSizes(new Dimension[] { hd1080 }); // 添加自定义分辨率
                webcam.setViewSize(hd1080); // 应用1080p分辨率
                webcam.open();

                isRunning = true;

                // 使用Timer定时更新视频帧
                timer = new javax.swing.Timer(33, e -> {
                    SwingUtilities.invokeLater(() -> {
                        videoPanel.repaint();
                    });
                });
                timer.start();

            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "无法启动摄像头: " + e.getMessage());
            }
        }
    }

    /**
     * 停止视频显示
     */
    private void stopVideo() {
        if (isRunning) {
            isRunning = false;
            if (timer != null) {
                timer.stop();
            }
            if (webcam != null) {
                webcam.close();
            }
        }
    }

    /**
     * 记录带坐标的事件
     * @param event 事件描述
     * @param x X坐标
     * @param y Y坐标
     */
    private void logEvent(String event, int x, int y) {
        String log = String.format("%s - 位置: (%d, %d)\n", event, x, y);
        eventLog.append(log);
        logArea.setText(eventLog.toString());
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    /**
     * 记录普通事件
     * @param event 事件描述
     */
    private void logEvent(String event) {
        String log = event + "\n";
        eventLog.append(log);
        logArea.setText(eventLog.toString());
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    /**
     * 获取鼠标按键名称
     * @param button 鼠标按键代码
     * @return 按键名称
     */
    private String getMouseButtonName(int button) {
        switch (button) {
            case MouseEvent.BUTTON1:
                return "左键";
            case MouseEvent.BUTTON2:
                return "中键";
            case MouseEvent.BUTTON3:
                return "右键";
            default:
                return "未知按键";
        }
    }

    /**
     * 进入全屏模式
     */
    private void enterFullScreen() {
        isFullScreen = true;
        logPanel.setVisible(false);
        controlPanel.setVisible(false);
        
        // 设置全屏控制面板位置
        fullScreenControls.setBounds((videoPanel.getWidth() - 200) / 2, 0, 200, 40);
        
        // 调整视频面板大小
        videoPanel.setPreferredSize(new Dimension(1920, 1080));
        
        // 进入全屏模式
        dispose(); // 先释放窗口
        setUndecorated(true); // 去掉窗口边框
        if (device.isFullScreenSupported()) {
            device.setFullScreenWindow(this);
        }
        setVisible(true); // 重新显示窗口
        
        // 刷新布局
        revalidate();
        repaint();
    }

    /**
     * 退出全屏模式
     */
    private void exitFullScreen() {
        isFullScreen = false;
        
        // 退出全屏模式
        dispose(); // 先释放窗口
        setUndecorated(false); // 恢复窗口边框
        device.setFullScreenWindow(null);
        setVisible(true); // 重新显示窗口
        
        logPanel.setVisible(isLogVisible);
        controlPanel.setVisible(true);
        
        // 恢复视频面板大小
        videoPanel.setPreferredSize(new Dimension(1280, 720));
        
        // 隐藏全屏控制面板
        fullScreenControls.setVisible(false);
        
        // 刷新布局
        revalidate();
        repaint();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new RemoteControlWindow().setVisible(true);
        });
    }
} 