package com.xycm.cqxf.call.service;

import android.app.KeyguardManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.mpttpnas.api.TrunkingCallSession;
import com.mpttpnas.pnaslibraryapi.callback.CallStateChangedCallbackEvent;
import com.xycm.cqxf.MyApplication;
import com.xycm.cqxf.R;
import com.xycm.cqxf.call.event.ShowCallReminderDialogEvent;
import com.xycm.cqxf.call.manager.CallManager;
import com.xycm.cqxf.service.MessageUiService;
import com.xycm.cqxf.util.InvState;
import com.xycm.cqxf.util.NotificationHelper;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * 保活服务
 */
public class KeepAliveService extends Service {
    private static final String TAG = "KeepAliveService";

    private NotificationHelper notificationHelper;
    private CallManager callManager;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // 通知相关常量
    private static final String CHANNEL_ID_KEEP_ALIVE = "keep_alive_channel";
    private static final String CHANNEL_ID_CALL = "incoming_call_channel";

    private PowerManager.WakeLock wakeLock;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate: 保活服务初始化");
        notificationHelper = new NotificationHelper(this);
        callManager = new CallManager(this, notificationHelper);
        initializeComponents();
    }

    /**
     * 每次startService()都会调用
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand: 服务命令执行");
        // 启动前台服务
        startForegroundService();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy: 服务被销毁");
        cleanup();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * 初始化组件
     */
    private void initializeComponents() {
        try {
            // 注册 EventBus
            if (!EventBus.getDefault().isRegistered(this)) {
                EventBus.getDefault().register(this);
            }
            // 创建通知渠道
            notificationHelper.createNotificationChannels();
            // 获取唤醒锁
            acquireWakeLock();
        } catch (Exception e) {
            Log.e(TAG, "initializeComponents: 初始化失败", e);
        }
    }

    /**
     * 清理资源
     */
    private void cleanup() {
        try {
            // 取消EventBus注册
            if (EventBus.getDefault().isRegistered(this)) {
                EventBus.getDefault().unregister(this);
            }

            // 释放唤醒锁
            releaseWakeLock();

            // 取消所有通知
            cancelAllNotifications();

        } catch (Exception e) {
            Log.e(TAG, "cleanup: 清理资源失败", e);
        }
    }

    /**
     * 启动前台服务
     */
    private void startForegroundService() {
        try {
            Notification notification = createKeepAliveNotification();
            startForeground(1, notification);
            Log.d(TAG, "startForegroundService: 前台服务已启动");
        } catch (Exception e) {
            Log.e(TAG, "startForegroundService: 启动前台服务失败", e);
        }
    }

    /**
     * 通话状态变化回调
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onCallStateChangedCallbackEvent(CallStateChangedCallbackEvent event) {
        if (event == null || event.getCallSession() == null) {
            Log.w(TAG, "onCallStateChangedCallbackEvent: 无效的通话事件");
            return;
        }
        boolean isAppInBackground = MyApplication.isAppInBackground();
        boolean isScreenLocked = isScreenLocked();
        Log.d(TAG, "handleCallStateChange: 屏幕锁定=" + isScreenLocked + ", 应用后台=" + isAppInBackground);
        // 前台通话
        if (!isAppInBackground && !isScreenLocked) {
            handleForegroundCall(event);
        } else {
            // 后台通话或振铃中
            if (event.getCallSession().isIncoming() && event.getCallSession().getCallState() == InvState.EARLY) {
                handleBackgroundCall(event.getCallSession());
            } else if (event.getCallSession().getCallState() == InvState.DISCONNECTED) {
                // 通话结束，取消通知
                cancelAllNotifications();
            }
        }
    }

    /**
     * 处理前台通话
     */
    private void handleForegroundCall(CallStateChangedCallbackEvent event) {
        try {
            ShowCallReminderDialogEvent dialogEvent = new ShowCallReminderDialogEvent(event.getCallId(), event.getCallSession());
            EventBus.getDefault().post(dialogEvent);
            Log.d(TAG, "handleForegroundCall: 已发送前台通话对话框事件");
        } catch (Exception e) {
            Log.e(TAG, "handleForegroundCall: 发送前台通话事件失败", e);
        }
    }

    /**
     * 处理后台通话
     */
    private void handleBackgroundCall(TrunkingCallSession callSession) {
        try {
            notificationHelper.showCustomCallNotification(callSession.getRemoteContact(), "来电", callSession);
        } catch (Exception e) {
            Log.e(TAG, "handleBackgroundCall: 处理后台通话失败", e);
            // 使用默认通知
            notificationHelper.showCustomCallNotification("未知组", "未知来电", callSession);
        }
    }

    /**
     * 创建保活通知
     */
    private Notification createKeepAliveNotification() {
        try {
            Intent notificationIntent = new Intent(this, MessageUiService.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);
            return new NotificationCompat.Builder(this, CHANNEL_ID_KEEP_ALIVE)
                    .setContentTitle("保活服务")
                    .setContentText("应用正在后台运行")
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentIntent(pendingIntent)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setOngoing(true)
                    .setWhen(System.currentTimeMillis())
                    .build();
        } catch (Exception e) {
            Log.e(TAG, "createKeepAliveNotification: 创建保活通知失败", e);
            return createFallbackNotification();
        }
    }

    /**
     * 创建降级通知
     */
    private Notification createFallbackNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID_KEEP_ALIVE)
                .setContentTitle("服务运行中")
                .setContentText("应用正在后台运行")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    /**
     * 检查屏幕是否锁定
     */
    private boolean isScreenLocked() {
        try {
            KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
            return keyguardManager != null && keyguardManager.isKeyguardLocked();
        } catch (Exception e) {
            Log.e(TAG, "isScreenLocked: 检查屏幕锁定状态失败", e);
            return false;
        }
    }

    /**
     * 获取唤醒锁
     */
    private void acquireWakeLock() {
        try {
            PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
            if (powerManager != null) {
                wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG + ":WakeLock");
                wakeLock.setReferenceCounted(false);
                if (!wakeLock.isHeld()) {
                    wakeLock.acquire(10 * 60 * 1000L);
                    Log.d(TAG, "acquireWakeLock: 唤醒锁已获取");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "acquireWakeLock: 获取唤醒锁失败", e);
        }
    }

    /**
     * 释放唤醒锁
     */
    private void releaseWakeLock() {
        try {
            if (wakeLock != null && wakeLock.isHeld()) {
                wakeLock.release();
                wakeLock = null;
                Log.d(TAG, "releaseWakeLock: 唤醒锁已释放");
            }
        } catch (Exception e) {
            Log.e(TAG, "releaseWakeLock: 释放唤醒锁失败", e);
        }
    }

    /**
     * 取消所有通知
     */
    private void cancelAllNotifications() {
        notificationHelper.cancelNotifications();
    }
}
