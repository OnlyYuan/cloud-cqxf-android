package com.mydemo.test31.util;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.SoundPool;

import java.io.IOException;

public class AudioPlayer {
    private MediaPlayer mediaPlayer; // 适合播放较长音频（如音乐）
    private SoundPool soundPool;     // 适合播放短音频（如提示音）
    private int soundId;             // SoundPool 音频ID

    // 初始化 SoundPool（短音频）
    public void initSoundPool(Context context, int rawResId) {
        if (soundPool == null) {
            // 配置音频属性
            AudioAttributes attributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION) // 通知类音频
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build();

            // 创建 SoundPool（最多同时播放1个音频）
            soundPool = new SoundPool.Builder()
                    .setAudioAttributes(attributes)
                    .setMaxStreams(1)
                    .build();

            // 加载音频资源
            soundId = soundPool.load(context, rawResId, 1);
        }
    }

    // 播放短音频（使用 SoundPool）
    public void playShortSound() {
        if (soundPool != null) {
            // 播放（音量：左1.0，右1.0，优先级1，循环0次，速率1.0）
            soundPool.play(soundId, 1.0f, 1.0f, 1, 0, 1.0f);
        }
    }

    // 播放长音频（使用 MediaPlayer，支持本地/网络资源）
    public void playLongAudio(Context context, int rawResId) {
        stopMediaPlayer(); // 先停止正在播放的音频

        mediaPlayer = MediaPlayer.create(context, rawResId);
        if (mediaPlayer != null) {
            // 设置循环播放（可选）
            mediaPlayer.setLooping(false);
            // 播放完成后释放资源
            mediaPlayer.setOnCompletionListener(mp -> stopMediaPlayer());
            // 开始播放
            mediaPlayer.start();
        }
    }

    // 播放网络音频（如 http 链接）
    public void playNetworkAudio(String url) {
        stopMediaPlayer();

        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(url);
            mediaPlayer.setAudioAttributes(
                    new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
            );
            mediaPlayer.prepareAsync(); // 异步准备，避免阻塞
            mediaPlayer.setOnPreparedListener(MediaPlayer::start);
            mediaPlayer.setOnCompletionListener(mp -> stopMediaPlayer());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 停止 MediaPlayer 播放并释放资源
    public void stopMediaPlayer() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    // 释放 SoundPool 资源
    public void releaseSoundPool() {
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }
    }
}