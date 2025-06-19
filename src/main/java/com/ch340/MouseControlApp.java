package com.ch340;

import com.fazecast.jSerialComm.SerialPort;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.io.IOException;

public class MouseControlApp extends JFrame {
    private JTextField xField;
    private JTextField yField;
    private JTextField stringField;
    private JButton moveButton;
    private JButton sendStringButton;
    private SerialPort serialPort;

    public MouseControlApp() {
        setTitle("鼠标控制程序");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(400, 300);
        setLocationRelativeTo(null);

        // 创建主面板
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // 创建鼠标控制面板
        JPanel mousePanel = new JPanel(new GridLayout(3, 2, 10, 10));
        mousePanel.setBorder(BorderFactory.createTitledBorder("鼠标控制"));

        // 添加鼠标控制组件
        mousePanel.add(new JLabel("X坐标:"));
        xField = new JTextField();
        mousePanel.add(xField);

        mousePanel.add(new JLabel("Y坐标:"));
        yField = new JTextField();
        mousePanel.add(yField);

        moveButton = new JButton("移动鼠标");
        mousePanel.add(moveButton);

        // 创建字符串发送面板
        JPanel stringPanel = new JPanel(new BorderLayout(10, 10));
        stringPanel.setBorder(BorderFactory.createTitledBorder("字符串发送"));
        
        stringField = new JTextField();
        sendStringButton = new JButton("发送字符串");
        
        stringPanel.add(stringField, BorderLayout.CENTER);
        stringPanel.add(sendStringButton, BorderLayout.SOUTH);

        // 添加面板到主面板
        mainPanel.add(mousePanel, BorderLayout.NORTH);
        mainPanel.add(stringPanel, BorderLayout.CENTER);

        // 添加按钮事件监听器
        moveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                moveMouse();
            }
        });

        sendStringButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendString();
            }
        });

        add(mainPanel);
        initializeSerialPort();
    }

    private void initializeSerialPort() {
        // 获取所有可用的串口
        SerialPort[] ports = SerialPort.getCommPorts();
        if (ports.length == 0) {
            JOptionPane.showMessageDialog(this, "未找到可用的串口设备！");
            return;
        }

        // 选择第一个串口（这里可以根据需要修改选择逻辑）
        serialPort = ports[0];
        serialPort.setBaudRate(9600);
        serialPort.setNumDataBits(8);
        serialPort.setNumStopBits(1);
        serialPort.setParity(SerialPort.NO_PARITY);

        if (!serialPort.openPort()) {
            JOptionPane.showMessageDialog(this, "无法打开串口：" + serialPort.getSystemPortName());
        }
    }

    private void moveMouse() {
        try {
            int x = Integer.parseInt(xField.getText());
            int y = Integer.parseInt(yField.getText());

            // 构建鼠标移动命令
            // 根据CH9329协议构建命令
            byte[] command = new byte[4];
            command[0] = (byte) 0x57;  // 命令头
            command[1] = (byte) x;     // X坐标
            command[2] = (byte) y;     // Y坐标
            command[3] = (byte) 0x00;  // 结束符

            if (serialPort != null && serialPort.isOpen()) {
                serialPort.writeBytes(command, command.length);
                System.out.println("发送指令成功");
            } else {
                JOptionPane.showMessageDialog(this, "串口未打开！");
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "请输入有效的数字！");
        }
    }

    private void sendString() {
        // String text = stringField.getText();
        // if (text.isEmpty()) {
        //     JOptionPane.showMessageDialog(this, "请输入要发送的字符串！");
        //     return;
        // }

        if (serialPort != null && serialPort.isOpen()) {
            try {
                byte aKeyCode = 0x04;
                //byte modifier = 0x00; // 如Shift请用0x02
                createKeyboardReportandSend(serialPort, aKeyCode, false, false);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "发送失败：" + e.getMessage());
            }
        } else {
            JOptionPane.showMessageDialog(this, "串口未打开！");
        }
    }

    public static void sendKey(SerialPort port, byte modifier, byte keyCode) throws IOException {
        // 构造数据包：包头(0x57,0xAB), Cmd(0x02), Len(0x08), Data(8字节), 校验和
        byte[] packet = new byte[13];
        packet[0] = 0x57;
        packet[1] = (byte)0xAB;
        packet[2] = 0x02;
        packet[3] = 0x08;
        packet[4] = modifier;    // Modifier
        packet[5] = 0x00;        // Reserved
        packet[6] = keyCode;     // 第1个普通按键
        // 剩余5个键码全为0
        Arrays.fill(packet, 7, 12, (byte)0x00);

        // 计算校验和：从Cmd(2)到最后一个数据字节
        int checksum = 0;
        for (int i = 2; i <= 11; i++) {
            checksum += (packet[i] & 0xFF);
        }
        packet[12] = (byte)(checksum & 0xFF);

        // 发送数据包
        port.getOutputStream().write(packet);
        port.getOutputStream().flush();
        System.out.println("发送成功");
    }

    public static void createKeyboardReportandSend(SerialPort port, byte keycode, boolean ctrl, boolean shift) {
        byte[] report = new byte[8];
        report[0] = (byte)0x57; // CH9329指令头
        report[1] = (byte)0xAB; // 固定前缀
        report[2] = (byte)0x00; // 设备地址（默认0）
        report[3] = (byte)0x02; // 键盘指令
        report[4] = (byte)(ctrl ? 0x01 : 0x00); // 修饰键
        report[5] = (byte)0x00; // 保留
        report[6] = keycode; // 按键值
        
        // 计算校验和
        int sum = 0;
        for (int i = 0; i < 7; i++) {
            sum += (report[i] & 0xFF);
        }
        report[7] = (byte)(0xFF - sum); // 校验和
        port.writeBytes(report, report.length);
        
        //return report;
    }
    byte[] createMouseReport(byte buttons, byte x, byte y) {
        byte[] report = new byte[8];
        report[0] = (byte)0x57;
        report[1] = (byte)0xAB;
        report[2] = (byte)0x00;
        report[3] = (byte)0x04; // 鼠标指令
        report[4] = buttons; // 按键状态
        report[5] = x;      // X位移
        report[6] = y;      // Y位移
        
        // 计算校验和
        int sum = 0;
        for (int i = 0; i < 7; i++) {
            sum += (report[i] & 0xFF);
        }
        report[7] = (byte)(0xFF - sum);
        return report;
    }
    
    

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new MouseControlApp().setVisible(true);
            }
        });
    }
} 