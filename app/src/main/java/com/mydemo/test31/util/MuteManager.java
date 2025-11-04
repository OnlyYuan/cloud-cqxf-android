package com.mydemo.test31.util;

import android.content.Context;
import android.media.AudioManager;

public class MuteManager {
    private AudioManager audioManager;

    // 初始化AudioManager
    public MuteManager(Context context) {
        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    }

    /**
     * 静音应用音乐流（通常应用播放的声音使用此流）
     * @param mute 是否静音
     */
    public void muteMusicStream(boolean mute) {
        if (audioManager != null) {
            // STREAM_MUSIC：用于音乐、游戏等应用的音频流
            audioManager.setStreamMute(AudioManager.STREAM_MUSIC, mute);
        }
    }

    /**
     * 将应用音乐流音量设置为0（替代静音的另一种方式）
     */
    public void setMusicVolumeToZero() {
        if (audioManager != null) {
            audioManager.setStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    0,  // 音量值，0为最小
                    AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE  // 操作标志
            );
        }
    }

    /**
     * 恢复音乐流音量到之前的水平
     * @param originalVolume 原始音量值
     */
    public void restoreMusicVolume(int originalVolume) {
        if (audioManager != null) {
            audioManager.setStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    originalVolume,
                    AudioManager.FLAG_ALLOW_RINGER_MODES
            );
        }
    }

    /**
     * 获取当前音乐流的音量
     */
    public int getCurrentMusicVolume() {
        if (audioManager != null) {
            return audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        }
        return 0;
    }
}

