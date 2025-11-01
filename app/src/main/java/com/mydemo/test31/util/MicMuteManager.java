package com.mydemo.test31.util;

import android.content.Context;
import android.media.AudioManager;
import android.os.Build;

public class MicMuteManager {
    private final AudioManager audioManager;
    private Context context;

    public MicMuteManager(Context context) {
        this.context = context;
        // 获取系统音频管理器
        this.audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    }

    /**
     * 麦克风静音（系统级）
     */
    public void muteMic() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android 6.0+：通过 setMicrophoneMute 直接静音
            audioManager.setMicrophoneMute(true);
        } else {
            // Android 6.0 以下：通过调整音频模式间接静音（兼容方案）
            audioManager.setMode(AudioManager.MODE_IN_CALL);
            audioManager.setStreamMute(AudioManager.STREAM_VOICE_CALL, true);
        }
    }

    /**
     * 取消麦克风静音（系统级）
     */
    public void unmuteMic() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            audioManager.setMicrophoneMute(false);
        } else {
            audioManager.setStreamMute(AudioManager.STREAM_VOICE_CALL, false);
            audioManager.setMode(AudioManager.MODE_NORMAL);
        }
    }

    /**
     * 判断麦克风是否处于静音状态
     */
    public boolean isMicMuted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return audioManager.isMicrophoneMute();
        } else {
            // 6.0 以下无直接判断方法，可通过音频模式间接判断
            return audioManager.getMode() == AudioManager.MODE_IN_CALL
                    && audioManager.isStreamMute(AudioManager.STREAM_VOICE_CALL);
        }
    }
}