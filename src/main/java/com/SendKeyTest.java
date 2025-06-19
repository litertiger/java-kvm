package com;
import com.fazecast.jSerialComm.SerialPort;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.io.IOException;
public class SendKeyTest {
    
 

    private SerialPort serialPort;

    public SendKeyTest() {
        initializeSerialPort();
    }
    
    private void initializeSerialPort() {
        // 获取所有可用的串口
        SerialPort[] ports = SerialPort.getCommPorts();
        if (ports.length == 0) {
            JOptionPane.showMessageDialog(null, "未找到可用的串口设备！");
            return;
        }

        // 选择第一个串口（这里可以根据需要修改选择逻辑）
        serialPort = ports[0];
        serialPort.setBaudRate(9600);
        serialPort.setNumDataBits(8);
        serialPort.setNumStopBits(1);
        serialPort.setParity(SerialPort.NO_PARITY);

        if (!serialPort.openPort()) {
            JOptionPane.showMessageDialog(null, "无法打开串口：" + serialPort.getSystemPortName());
        }
    }

    public void sendKey()  
        {
        try{
        // 构造数据包：包头(0x57,0xAB), Cmd(0x02), Len(0x08), Data(8字节), 校验和
        byte[] packet = new byte[14];
        packet[0] = 0x57;
        packet[1] = (byte)0xAB;
        packet[2] = 0x00;
        packet[3] = 0x02;
        packet[4] = 0x08;
        packet[5] = 0x00;    // Modifier
        packet[6] = 0x00;        // Reserved
        packet[7] = 0x04;     // 第1个普通按键A
        // 剩余5个键码全为0
        Arrays.fill(packet, 8, 13, (byte)0x00);

      
        int checksum = 0;
        for (int i = 0; i <= 12; i++) {
            checksum += (packet[i] & 0xFF);
        }
        packet[13] = (byte)(checksum & 0xFF);
        System.out.println(bytesToHex(packet));

        // 发送数据包

        serialPort.writeBytes(packet, packet.length);
        byte[] readBuffer = new byte[1024];
        int numRead = serialPort.readBytes(readBuffer, readBuffer.length);
        if (numRead > 0) {
            byte[] response = Arrays.copyOf(readBuffer, numRead);
            System.out.println("收到返回数据：" + bytesToHex(response));
        }

        Thread.sleep(10);
        //====================================================
        packet[7] = 0x00; 
          // 剩余5个键码全为0
          Arrays.fill(packet, 7, 12, (byte)0x00);
           // 计算校验和：从Cmd(2)到最后一个数据字节
           checksum = 0;
           for (int i = 0; i <= 12; i++) {
               checksum += (packet[i] & 0xFF);
           }
           packet[13] = (byte)(checksum & 0xFF);
           System.out.println(bytesToHex(packet));
           serialPort.writeBytes(packet, packet.length);
        System.out.println("发送成功");
        readBuffer = new byte[1024];
         numRead = serialPort.readBytes(readBuffer, readBuffer.length);
        if (numRead > 0) {
            byte[] response = Arrays.copyOf(readBuffer, numRead);
            System.out.println("收到返回数据：" + bytesToHex(response));
        }

    }
    catch(Exception e){
        JOptionPane.showMessageDialog(null, "发送失败：" + e.getMessage());
    }

        }

        private static String bytesToHex(byte[] bytes) {
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02X ", b));
            }
            return sb.toString().trim();
        }

    public static void main(String[] args) {
        SendKeyTest sendKeyTest = new SendKeyTest();


        sendKeyTest.sendKey();
    }
    
}
