package com.ch340;

import com.fazecast.jSerialComm.SerialPort;
import javax.swing.*;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;

public class CH9329Serial {
    private SerialPort serialPort;

   

    

    public CH9329Serial() {
        initializeSerialPort();
    }

    private void initializeSerialPort() {
        SerialPort[] ports = SerialPort.getCommPorts();
        if (ports.length == 0) {
            JOptionPane.showMessageDialog(null, "未找到可用的串口设备！");
            return;
        }
        serialPort = ports[0];
        serialPort.setBaudRate(9600);
        serialPort.setNumDataBits(8);
        serialPort.setNumStopBits(1);
        serialPort.setParity(SerialPort.NO_PARITY);

        if (!serialPort.openPort()) {
            JOptionPane.showMessageDialog(null, "无法打开串口：" + serialPort.getSystemPortName());
        }
    }

    // 发送数据包并自动读取回响应
    public synchronized void sendPacket(byte[] packet) throws Exception {
        serialPort.writeBytes(packet, packet.length);

        // 回读应答（可按协议自行调整）
        byte[] readBuffer = new byte[1024];
        int numRead = serialPort.readBytes(readBuffer, readBuffer.length);
        if (numRead > 0) {
            byte[] response = Arrays.copyOf(readBuffer, numRead);
            System.out.println("收到返回数据：" + bytesToHex(response));
        }
    }

    // 构造完整数据包
    public static byte[] buildPacket(int cmd, byte[] data) {
        int len = data.length;
        byte[] packet = new byte[5 + len + 1]; // 2头 + 2cmd + 1len + data + 1sum
        packet[0] = 0x57;
        packet[1] = (byte) 0xAB;
        packet[2] = 0x00;
        packet[3] = (byte) cmd;
        packet[4] = (byte) len;
        System.arraycopy(data, 0, packet, 5, len);

        int checksum = 0;
        for (int i = 0; i < packet.length - 1; i++)
            checksum += packet[i] & 0xFF;
        packet[packet.length - 1] = (byte) (checksum & 0xFF);

        return packet;
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02X ", b));
        return sb.toString().trim();
    }
}

// ======= 键盘控制类 =========
class KeyboardCommander {
    private final CH9329Serial serial;

    public static  Map<String, Byte> KEYCODE_MAP = new HashMap<>();

    // 初始化字母键码
    static {
        for (char c = 'A'; c <= 'Z'; c++) {
            // 'a' 对应 0x04，依次递增，到 'z'
            KEYCODE_MAP.put(c+"", (byte)(0x04 + (c - 'A')));
        }
        KEYCODE_MAP.put("Enter", (byte)0x58);
      
        KEYCODE_MAP.put("Delete", (byte)0x4C);
        KEYCODE_MAP.put("Escape", (byte)0x29);
        KEYCODE_MAP.put("Tab", (byte)0x2B);
        KEYCODE_MAP.put("CapsLock", (byte)0x39);
        KEYCODE_MAP.put("Shift", (byte)0xE5);
        KEYCODE_MAP.put("Space", (byte)0x2C);
        KEYCODE_MAP.put("F1", (byte)0x3A);
        KEYCODE_MAP.put("F2", (byte)0x3B);
        KEYCODE_MAP.put("F3", (byte)0x3C);
        KEYCODE_MAP.put("F4", (byte)0x3D);
        KEYCODE_MAP.put("F5", (byte)0x3E);
        KEYCODE_MAP.put("F6", (byte)0x3F);
        KEYCODE_MAP.put("F7", (byte)0x40);
        KEYCODE_MAP.put("F8", (byte)0x41);  
        KEYCODE_MAP.put("F9", (byte)0x42);
        KEYCODE_MAP.put("F10", (byte)0x43);
        KEYCODE_MAP.put("F11", (byte)0x44);
        KEYCODE_MAP.put("F12", (byte)0x45);
        KEYCODE_MAP.put("1", (byte)0x1E);
        KEYCODE_MAP.put("2", (byte)0x1F);
        KEYCODE_MAP.put("3", (byte)0x20);
        KEYCODE_MAP.put("4", (byte)0x21);
        KEYCODE_MAP.put("5", (byte)0x22);
        KEYCODE_MAP.put("6", (byte)0x23);
        KEYCODE_MAP.put("7", (byte)0x24);
        KEYCODE_MAP.put("8", (byte)0x25);
        KEYCODE_MAP.put("9", (byte)0x26);
        KEYCODE_MAP.put("0", (byte)0x27);
        KEYCODE_MAP.put("-", (byte)0x2D);
        KEYCODE_MAP.put("=", (byte)0x2E);
        KEYCODE_MAP.put("Backspace", (byte)0x2A);

        KEYCODE_MAP.put("Insert", (byte)0x49);
        KEYCODE_MAP.put("Home", (byte)0x5F);
        KEYCODE_MAP.put("End", (byte)0x4D);
        KEYCODE_MAP.put("PageUp", (byte)0x4B);
        KEYCODE_MAP.put("PageDown", (byte)0x4E);
        KEYCODE_MAP.put("Up", (byte)0x60);
        KEYCODE_MAP.put("Down", (byte)0x5A);
        KEYCODE_MAP.put("←", (byte)0x5C);
        KEYCODE_MAP.put("→", (byte)0x5E);


        KEYCODE_MAP.put(",", (byte)0x36);
        KEYCODE_MAP.put(".", (byte)0x37);
        KEYCODE_MAP.put("/", (byte)0x54);

        KEYCODE_MAP.put(";", (byte)0x33);
        KEYCODE_MAP.put("'", (byte)0x34);

        KEYCODE_MAP.put("`", (byte)0x235);

        KEYCODE_MAP.put("[", (byte)0x2F);
        KEYCODE_MAP.put("]", (byte)0x30);
         KEYCODE_MAP.put("\\", (byte)0x31);
      

        



        
        


    
    }

    public KeyboardCommander(CH9329Serial serial) {
        this.serial = serial;
    }

    /**
     * 发送单个按键或组合键
     * @param modifier 修饰键Ctrl/Shift/Alt等, 0-不按，见下表
     * @param keyCodes 按下的主键(最多6个), 参考协议表（如A=0x04, Enter=0x28）
     */
    public void sendKeys(int modifier, int... keyCodes) throws Exception {//需要将press和release分开，因为release的时候，需要将modifier也释放掉
        byte[] data = new byte[8];
        data[0] = (byte) modifier;
        data[1] = 0x00; // 保留
        for (int i = 0; i < Math.min(keyCodes.length, 6); i++) {
            data[2 + i] = (byte) keyCodes[i];
        }
        // 发送按下包
        serial.sendPacket(CH9329Serial.buildPacket(CH9329Cmd.WRITE_KEY_CMD, data));
        Thread.sleep(10);

        // 发送释放包
        Arrays.fill(data, (byte) 0);
        serial.sendPacket(CH9329Serial.buildPacket(CH9329Cmd.WRITE_KEY_CMD, data));
    }
    public void pressKeys(int modifier, int... keyCodes) throws Exception {//需要将press和release分开，因为release的时候，需要将modifier也释放掉
        byte[] data = new byte[8];
        data[0] = (byte) modifier;
        data[1] = 0x00; // 保留
        for (int i = 0; i < Math.min(keyCodes.length, 6); i++) {
            data[2 + i] = (byte) keyCodes[i];
        }
        // 发送按下包
        serial.sendPacket(CH9329Serial.buildPacket(CH9329Cmd.WRITE_KEY_CMD, data));
    

    }
    public void releaseKeys(int modifier, int... keyCodes) throws Exception {//需要将press和release分开，因为release的时候，需要将modifier也释放掉
        byte[] data = new byte[8];
        data[0] = (byte) modifier;
        data[1] = 0x00; // 保留
        for (int i = 0; i < Math.min(keyCodes.length, 6); i++) {
            data[2 + i] = (byte) keyCodes[i];
        }
        
        // 发送释放包
        Arrays.fill(data, (byte) 0);
        serial.sendPacket(CH9329Serial.buildPacket(CH9329Cmd.WRITE_KEY_CMD, data));
    }

    // 直接封装常见功能
    public void sendKey(String key) throws Exception      { 
         if (KEYCODE_MAP.containsKey(key)) {
            sendKeys(0, KEYCODE_MAP.get(key)); 
         }
        
    }
    public void pressKey(String key) throws Exception      { 
       
        if (KEYCODE_MAP.containsKey(key)) {
            pressKeys(0, KEYCODE_MAP.get(key)); 
         }
    }
    public void releaseKey(String key) throws Exception      { 
        if (KEYCODE_MAP.containsKey(key)) {
            releaseKeys(0, KEYCODE_MAP.get(key)); 
         }
    }
    public void sendA() throws Exception      { sendKeys(0, 0x04); }
    public void sendCtrlC() throws Exception  { sendKeys(1, 0x06); } // 1=Ctrl, 0x06=C
    public void sendAltTab() throws Exception { sendKeys(4, 0x2B); } // 4=Alt, 0x2B=Tab
    // 你可以根据协议扩展更多组合键
}

// ======= 鼠标控制类 ===========
class MouseCommander {
    private final CH9329Serial serial;

    public MouseCommander(CH9329Serial serial) {
        this.serial = serial;
    }

    /**
     * 鼠标移动与按键命令
     * @param button 按钮，1=左键,2=右键,4=中键。
     */
    public void RelClickMouse(int button) throws Exception {
        byte[] data = new byte[5];
        data[0] = 0x01;//这个必须是01
        data[1] = (byte) button;//01 是 左 ，02 是 右 ，04 是 中
        data[2] = 0x00;//x 轴不动
        data[3] = 0x00 ;//y 轴不动
        data[4] = 0x00 ;//齿轮不滚动
        serial.sendPacket(CH9329Serial.buildPacket(CH9329Cmd.CLICK_MOUSE_CMD, data));

        // 释放鼠标
        data[0] = 0x01;//这个必须是01
        data[1] = 0x00;//01 是 左 ，02 是 右 ，04 是 中,00 表示释放
        data[2] = 0x00;//x 轴不动
        data[3] = 0x00 ;//y 轴不动
        data[4] = 0x00 ;//齿轮不滚动
        serial.sendPacket(CH9329Serial.buildPacket(CH9329Cmd.CLICK_MOUSE_CMD, data));
    }

    public void AbsMoveMouse(int x, int y) throws Exception {//绝对移动,但是不带按键动作
        byte[] data = new byte[7];
        data[0] = 0x02;//这个必须是02
        data[1] = 0x00;//01 是 左 ，02 是 右 ，04 是 中,00 表示释放，或者未按下
        int newx=(x*4096)/1920;
        int newy=(y*4096)/1080;
        int x1=newx&0xFF;
        int x2=(newx>>8)&0xFF;
        int y1=newy&0xFF;
        int y2=(newy>>8)&0xFF;
        data[2] = (byte) x1;//x 轴不动
        data[3] = (byte) x2;//x 轴不动
        data[4] = (byte) y1;//y 轴不动 
        data[5] = (byte) y2;//y 轴不动
        data[6] = 0x00 ;//齿轮不滚动
        serial.sendPacket(CH9329Serial.buildPacket(CH9329Cmd.ABS_MOVE_MOUSE_CMD, data));
    }

    public void AbsClickMouse(String buttonname,int x, int y) throws Exception {//绝对移动,但是不带按键动作
        byte[] data = new byte[7];
        data[0] = 0x02;//这个必须是02
        if(buttonname.equals("左键")){
            data[1] = 0x01;//01 是 左 ，02 是 右 ，04 是 中,00 表示释放，或者未按下
        }else if(buttonname.equals("右键")){
            data[1] = 0x02;//01 是 左 ，02 是 右 ，04 是 中,00 表示释放，或者未按下
        }else if(buttonname.equals("中键")){
            data[1] = 0x04;//01 是 左 ，02 是 右 ，04 是 中,00 表示释放，或者未按下
        }

        int newx=(x*4096)/1920;
        int newy=(y*4096)/1080;
        int x1=newx&0xFF;
        int x2=(newx>>8)&0xFF;
        int y1=newy&0xFF;
        int y2=(newy>>8)&0xFF;
        data[2] = (byte) x1;//x 轴不动
        data[3] = (byte) x2;//x 轴不动
        data[4] = (byte) y1;//y 轴不动 
        data[5] = (byte) y2;//y 轴不动
        data[6] = 0x00 ;//齿轮不滚动
        serial.sendPacket(CH9329Serial.buildPacket(CH9329Cmd.ABS_MOVE_MOUSE_CMD, data));
    }
    public void AbsReleaseMouse(String buttonname,int x, int y) throws Exception {//绝对移动,但是不带按键动作
        byte[] data = new byte[7];
        data[0] = 0x02;//这个必须是02
        data[1] = 0x00;//01 是 左 ，02 是 右 ，04 是 中,00 表示释放，或者未按下
        int newx=(x*4096)/1920;
        int newy=(y*4096)/1080;
        int x1=newx&0xFF;
        int x2=(newx>>8)&0xFF;
        int y1=newy&0xFF;
        int y2=(newy>>8)&0xFF;
        data[2] = (byte) x1;//x 轴不动
        data[3] = (byte) x2;//x 轴不动
        data[4] = (byte) y1;//y 轴不动 
        data[5] = (byte) y2;//y 轴不动
        data[6] = 0x00 ;//齿轮不滚动
        serial.sendPacket(CH9329Serial.buildPacket(CH9329Cmd.ABS_MOVE_MOUSE_CMD, data));
    }
    

}

// ======= 协议命令常量 ===========
class CH9329Cmd {
    public static final int WRITE_KEY_CMD = 0x02;    // 写键盘
    public static final int CLICK_MOUSE_CMD = 0x05;  // 写鼠标 按键动作
    public static final int ABS_MOVE_MOUSE_CMD = 0x04;  // 写鼠标 移动
    
    // 你还可以补充其它命令（参考协议文档）
}

// =========== 实际调用测试 ==========
class UseDemo {
    public static void main(String[] args) throws Exception {
        CH9329Serial serial = new CH9329Serial();
        KeyboardCommander key = new KeyboardCommander(serial);
        MouseCommander mouse = new MouseCommander(serial);
        // System.out.println(String.format("%02X ", (byte)KeyboardCommander.KEYCODE_MAP.get("A")));

        for(String c : KeyboardCommander.KEYCODE_MAP.keySet()){
            System.out.println("sendKey:"+c);
            key.sendKey(c);
            // Thread.sleep(200);
        }

        // mouse.clickRight();
        // Thread.sleep(1000);
        // mouse.clickLeft();
       // key.sendA();
        //mouse.AbsMoveMouse(100, 600);
    
       
    }
}