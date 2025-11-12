package com.mydemo.test31.util;

/**
 * 通话状态常量类
 */
public class InvState {

    public static final int INVALID = -1;      // 无效状态
    public static final int NULL = 0;          // 空状态/初始状态
    public static final int CALLING = 1;       // 呼叫中（主叫方）
    public static final int INCOMING = 2;      // 来电中（被叫方）
    public static final int EARLY = 3;         // 早期媒体/振铃中
    public static final int CONNECTING = 4;    // 连接中
    public static final int CONFIRMED = 5;     // 已确认/通话中
    public static final int DISCONNECTED = 6;  // 已断开
    public static final int HANGUPING = 7;     // 挂断中

    private InvState() {
        // 私有构造函数，防止实例化
    }
}