package com.mydemo.test31.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.mpttpnas.api.TrunkingCallSession;
import com.mpttpnas.pnaslibraryapi.callback.CallStateChangedCallbackEvent;
import com.mydemo.test31.MessageUiActivity;
import com.mydemo.test31.R;
import com.mydemo.test31.util.AudioPlayer;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class KeepAliveService extends Service {
    private static final String CHANNEL_ID = "keep_alive_channel";
    private static final int NOTIFICATION_ID = 1001;
    private String TAG ="KeepAliveService";
    private PowerManager.WakeLock wakeLock; // 唤醒锁，减少系统休眠
    private AudioPlayer audioPlayer;

    @Override
    public void onCreate() {
        super.onCreate();
        EventBus.getDefault().register(this);
        // 初始化通知渠道（Android 8.0+ 必需）
        createNotificationChannel();
        // 获取唤醒锁（可选，减少休眠）
        acquireWakeLock();
        startForeground(NOTIFICATION_ID, createNotificationForBackground());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // 返回 START_STICKY：系统杀死后会尝试重启 Service
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null; // 无需绑定，返回 null
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // 释放唤醒锁
        releaseWakeLock();
        // （可选）如果被销毁，尝试重启自己
        Intent intent = new Intent(this, KeepAliveService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }

    // 创建通知渠道（Android 8.0+ 必需）
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "后台保活服务", // 通知渠道名称（用户可见）
                    NotificationManager.IMPORTANCE_LOW // 低优先级，避免打扰用户
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    TrunkingCallSession callSession;
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onCallStateChangedCallbackEvent(CallStateChangedCallbackEvent event){
        callSession = event.getCallSession();

        Log.i(TAG,"-->account getGroupCallUdn:"+callSession.getGroupCallUdn()
                +"floorName:"+callSession.getFloorName()
                +"videoName；"+callSession.getVideoName()
                +"floorUdn"+callSession.getFloorUdn()
                +"callId"+ event.getCallId()
        );
        if(callSession != null && !callSession.isAfterEnded()){
            if(callSession.isIncoming() && callSession.isBeforeConfirmed()){
                Log.i(TAG,"--->还未接听");
                // 启动前台服务（显示通知）
                startForeground(NOTIFICATION_ID, createNotification());
            }else{//接听

            }
        }else{
            Log.i(TAG,"挂断了");
        }
    }

    // 创建前台通知
    private Notification createNotification() {
        // 点击通知跳转的页面（可选）
        Intent intent = new Intent(this, MessageUiActivity.class);
        intent.putExtra("callSession",callSession);
        intent.putExtra("comeType",1);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        // 构建通知
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("消息来电")
                .setContentText("点击接听")
                .setSmallIcon(R.mipmap.ic_launcher) // 必需设置图标
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private Notification createNotificationForBackground() {
        // 构建通知
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("警务通后台运行中")
                .setSmallIcon(R.mipmap.ic_launcher) // 必需设置图标
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    // 获取唤醒锁（防止系统休眠导致 Service 暂停）
    private void acquireWakeLock() {
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        if (powerManager != null) {
            wakeLock = powerManager.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "KeepAliveService:WakeLock"
            );
            // 非永久持有，系统低电量时可能释放
            if (!wakeLock.isHeld()) {
                wakeLock.acquire(10 * 60 * 1000L); // 持有 10 分钟，可按需调整
            }
        }
    }

    // 释放唤醒锁
    private void releaseWakeLock() {
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
            wakeLock = null;
        }
    }


}