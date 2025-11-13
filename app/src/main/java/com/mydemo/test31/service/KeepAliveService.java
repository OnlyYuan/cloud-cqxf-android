package com.mydemo.test31.service;

import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.core.app.NotificationCompat;

import com.google.android.gms.common.util.CollectionUtils;
import com.mpttpnas.api.TrunkingCallSession;
import com.mpttpnas.api.TrunkingGroupContact;
import com.mpttpnas.api.TrunkingLocalContact;
import com.mpttpnas.pnaslibraryapi.PnasContactUtil;
import com.mpttpnas.pnaslibraryapi.callback.CallStateChangedCallbackEvent;
import com.mydemo.test31.MyApplication;
import com.mydemo.test31.R;
import com.mydemo.test31.activity.MainActivity;
import com.mydemo.test31.callback.CallReceiver;
import com.mydemo.test31.event.ShowCallReminderDialogEvent;
import com.mydemo.test31.util.InvState;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;
import java.util.Objects;

public class KeepAliveService extends Service {
    private static final String TAG = "KeepAliveService";

    // 通知相关常量
    private static final String CHANNEL_ID_KEEP_ALIVE = "keep_alive_channel";
    private static final String CHANNEL_ID_CALL = "incoming_call_channel";
    private static final String CHANNEL_NAME_KEEP_ALIVE = "保活服务";
    private static final String CHANNEL_NAME_CALL = "来电通知";
    private static final String CHANNEL_DESC_KEEP_ALIVE = "应用后台运行状态";
    private static final String CHANNEL_DESC_CALL = "来电通知提醒";

    // 通知ID
    private static final int NOTIFICATION_ID_KEEP_ALIVE = 1;
    public static final int NOTIFICATION_ID_CALL = 1002;

    // 请求码
    private static final int REQUEST_CODE_ANSWER = 100;
    private static final int REQUEST_CODE_REJECT = 101;
    private static final int REQUEST_CODE_MAIN = 102;

    // 时间常量
    private static final long WAKE_LOCK_TIMEOUT = 10 * 60 * 1000L; // 10分钟
    private static final long NOTIFICATION_TIMEOUT_MS = 30000; // 30秒
    private static final long[] VIBRATION_PATTERN = {0, 500, 200, 500};

    // Intent Actions
    public static final String ACTION_ANSWER_CALL = "com.mydemo.test31.ACTION_ANSWER_CALL";
    public static final String ACTION_REJECT_CALL = "com.mydemo.test31.ACTION_REJECT_CALL";
    private static final String ACTION_SHOW_CALL_NOTIFICATION = "SHOW_CALL_NOTIFICATION";

    // Extra keys
    private static final String EXTRA_CALLER_NAME = "caller_name";
    private static final String EXTRA_CALLER_NUMBER = "caller_number";

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private PowerManager.WakeLock wakeLock;
    private TrunkingCallSession callSession;
    private NotificationManager notificationManager;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate: 保活服务初始化");
        initializeComponents();
    }

    /**
     * 每次startService()都会调用
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand: 服务命令执行");

        // 启动前台服务
        // startForegroundService();

        // 处理Intent
        // handleIntent(intent);

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy: 服务被销毁");
        cleanup();
        super.onDestroy();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Log.d(TAG, "onTaskRemoved: 应用从最近任务移除");
        super.onTaskRemoved(rootIntent);
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

            // 初始化通知管理器
            notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            // 创建通知渠道
            createNotificationChannels();

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
            startForeground(NOTIFICATION_ID_KEEP_ALIVE, notification);
            Log.d(TAG, "startForegroundService: 前台服务已启动");
        } catch (Exception e) {
            Log.e(TAG, "startForegroundService: 启动前台服务失败", e);
        }
    }

    /**
     * 处理Intent
     */
    private void handleIntent(Intent intent) {
        if (intent == null) {
            return;
        }

        String action = intent.getAction();
        if (ACTION_SHOW_CALL_NOTIFICATION.equals(action)) {
            String callerName = intent.getStringExtra(EXTRA_CALLER_NAME);
            String callerNumber = intent.getStringExtra(EXTRA_CALLER_NUMBER);
            showCustomCallNotification(callerName, callerNumber);
        }
    }

    /**
     * EventBus事件处理 - 通话状态改变
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onCallStateChangedCallbackEvent(CallStateChangedCallbackEvent event) {
        if (event == null || event.getCallSession() == null) {
            Log.w(TAG, "onCallStateChangedCallbackEvent: 无效的通话事件");
            return;
        }

        callSession = event.getCallSession();
        handleCallStateChange(event);
    }

    /**
     * 处理通话状态变化
     */
    private void handleCallStateChange(CallStateChangedCallbackEvent event) {
        callSession = event.getCallSession();
        if (Objects.isNull(callSession)) {
            return;
        }
        boolean isScreenLocked = isScreenLocked();
        boolean isAppInBackground = MyApplication.isAppInBackground();
        Log.d(TAG, "handleCallStateChange: 屏幕锁定=" + isScreenLocked + ", 应用后台=" + isAppInBackground);
        callSession = event.callSession;
        if (!isAppInBackground && !isScreenLocked) {
            handleForegroundCall(event);
        } else {
            if (!callSession.isAfterEnded()) {
                // 早期媒体/振铃中
                if (callSession.isIncoming() && callSession.isBeforeConfirmed()
                        && callSession.getCallState() == InvState.EARLY) {
                    handleBackgroundCall();
                } else if (callSession.getCallState() == InvState.CONFIRMED) {
                    return;
                }
            } else if (callSession.getCallState() == InvState.DISCONNECTED) {
                // 关闭通知
                cancelAllNotifications();
            }
        }
    }

    /**
     * 处理前台通话
     */
    private void handleForegroundCall(CallStateChangedCallbackEvent event) {
        try {
            ShowCallReminderDialogEvent dialogEvent = new ShowCallReminderDialogEvent(event.callId, event.callSession);
            EventBus.getDefault().post(dialogEvent);
            Log.d(TAG, "handleForegroundCall: 已发送前台通话对话框事件");
        } catch (Exception e) {
            Log.e(TAG, "handleForegroundCall: 发送前台通话事件失败", e);
        }
    }

    /**
     * 处理后台通话
     */
    private void handleBackgroundCall() {
        try {
            ContactInfo contactInfo = findContactInfo();
            showCustomCallNotification(contactInfo.groupName, contactInfo.contactName);
        } catch (Exception e) {
            Log.e(TAG, "handleBackgroundCall: 处理后台通话失败", e);
            // 降级处理：显示默认通知
            showCustomCallNotification("", "未知来电");
        }
    }

    /**
     * 查找联系人信息
     */
    private ContactInfo findContactInfo() {
        ContactInfo contactInfo = new ContactInfo();

        try {
            List<TrunkingGroupContact> groupList = PnasContactUtil.getInstance().getMyGroupList();
            if (CollectionUtils.isEmpty(groupList)) {
                contactInfo.contactName = callSession.getRemoteContact() != null ?
                        callSession.getRemoteContact() : "未知联系人";
                return contactInfo;
            }

            String remoteContact = callSession.getRemoteContact();

            // 遍历所有组和成员
            for (TrunkingGroupContact groupContact : groupList) {
                List<TrunkingLocalContact> memberList = PnasContactUtil
                        .getInstance().getGroupContactList(groupContact.getGroupGdn());

                if (CollectionUtils.isEmpty(memberList)) {
                    continue;
                }

                // 在当前组的成员中查找
                for (TrunkingLocalContact contact : memberList) {
                    if (contact.getUdn().equals(remoteContact)) {
                        contactInfo.contactName = contact.getName();
                        contactInfo.groupName = groupContact.getGroupName();
                        break;
                    }
                }

                if (!TextUtils.isEmpty(contactInfo.contactName)) {
                    break;
                }
            }

            // 设置默认值
            if (TextUtils.isEmpty(contactInfo.contactName)) {
                contactInfo.contactName = remoteContact != null ? remoteContact : "未知联系人";
            }
            if (TextUtils.isEmpty(contactInfo.groupName)) {
                contactInfo.groupName = "未知组";
            }

        } catch (Exception e) {
            Log.e(TAG, "findContactInfo: 查找联系人信息失败", e);
            contactInfo.contactName = "未知联系人";
            contactInfo.groupName = "未知组";
        }

        return contactInfo;
    }

    /**
     * 创建保活通知
     */
    private Notification createKeepAliveNotification() {
        try {
            Intent notificationIntent = new Intent(this, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this,
                    0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);
            return new NotificationCompat.Builder(this, CHANNEL_ID_KEEP_ALIVE)
                    .setContentTitle(getString(R.string.keep_alive_title))
                    .setContentText(getString(R.string.keep_alive_content))
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
     * 创建通知渠道
     */
    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        try {
            // 保活服务通知渠道
            NotificationChannel keepAliveChannel = new NotificationChannel(
                    CHANNEL_ID_KEEP_ALIVE,
                    CHANNEL_NAME_KEEP_ALIVE,
                    NotificationManager.IMPORTANCE_LOW
            );
            keepAliveChannel.setDescription(CHANNEL_DESC_KEEP_ALIVE);
            keepAliveChannel.setShowBadge(false);

            // 来电通知渠道
            NotificationChannel callChannel = new NotificationChannel(
                    CHANNEL_ID_CALL,
                    CHANNEL_NAME_CALL,
                    NotificationManager.IMPORTANCE_HIGH
            );
            callChannel.setDescription(CHANNEL_DESC_CALL);
            callChannel.setVibrationPattern(VIBRATION_PATTERN);
            callChannel.enableVibration(true);
            callChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            callChannel.setShowBadge(true);

            if (notificationManager != null) {
                notificationManager.createNotificationChannel(keepAliveChannel);
                notificationManager.createNotificationChannel(callChannel);
                Log.d(TAG, "createNotificationChannels: 通知渠道创建成功");
            }

        } catch (Exception e) {
            Log.e(TAG, "createNotificationChannels: 创建通知渠道失败", e);
        }
    }

    /**
     * 显示自定义来电通知
     */
    private void showCustomCallNotification(String callerName, String callerNumber) {
        try {
            // 参数验证
            if (callerNumber == null || callerNumber.isEmpty()) {
                Log.w(TAG, "showCustomCallNotification: 来电号码为空");
                callerNumber = "未知号码";
            }

            if (notificationManager == null) {
                Log.e(TAG, "showCustomCallNotification: NotificationManager为空");
                return;
            }

            // 创建自定义布局
            RemoteViews remoteViews = createNotificationView(callerName, callerNumber);

            // 构建通知
            Notification notification = buildCallNotification(remoteViews, callerName, callerNumber);

            // 显示通知
            notificationManager.notify(NOTIFICATION_ID_CALL, notification);

            Log.d(TAG, "showCustomCallNotification: 来电通知已显示 - " + callerNumber);

        } catch (Exception e) {
            Log.e(TAG, "showCustomCallNotification: 显示来电通知失败", e);
        }
    }

    /**
     * 创建通知视图
     */
    private RemoteViews createNotificationView(String callerName, String callerNumber) {
        RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.custom_call_notification);
        // 设置通知内容
        remoteViews.setTextViewText(R.id.notification_title, getString(R.string.incoming_call_title));
        String contentText = buildContentText(callerName, callerNumber);
        remoteViews.setTextViewText(R.id.notification_content, contentText);
        // 设置按钮点击事件
        setupButtonActions(remoteViews);

        return remoteViews;
    }

    /**
     * 构建内容文本
     */
    private String buildContentText(String callerName, String callerNumber) {
        if (!TextUtils.isEmpty(callerName) && !callerName.equals(callerNumber)) {
            return callerName + " 来电\n" + callerNumber;
        } else {
            return callerNumber + " 来电";
        }
    }

    /**
     * 设置按钮动作
     */
    private void setupButtonActions(RemoteViews remoteViews) {
        try {
            // 接听按钮
            Intent answerIntent = CallReceiver.createAnswerIntent(this, callSession);
            PendingIntent answerPendingIntent = PendingIntent.getBroadcast(
                    this,
                    generateRequestCode("answer"),
                    answerIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            remoteViews.setOnClickPendingIntent(R.id.btn_accept, answerPendingIntent);

            // 拒绝按钮
            Intent rejectIntent = CallReceiver.createRejectIntent(this, callSession);
            PendingIntent rejectPendingIntent = PendingIntent.getBroadcast(
                    this,
                    generateRequestCode("reject"),
                    rejectIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            remoteViews.setOnClickPendingIntent(R.id.btn_reject, rejectPendingIntent);

        } catch (Exception e) {
            Log.e(TAG, "setupButtonActions: 设置按钮动作失败", e);
        }
    }

    /**
     * 构建通话通知
     */
    private Notification buildCallNotification(RemoteViews remoteViews, String callerName, String callerNumber) {
        PendingIntent mainPendingIntent = createMainPendingIntent(callerName, callerNumber);
        Bitmap largeIcon = loadAppIcon();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID_CALL)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setCustomContentView(remoteViews)
                .setCustomBigContentView(remoteViews)
                .setContentIntent(mainPendingIntent)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setAutoCancel(true)
                .setVibrate(VIBRATION_PATTERN)
                .setDefaults(Notification.DEFAULT_SOUND)
                .setOngoing(true)
                .setFullScreenIntent(mainPendingIntent, true)
                .setTimeoutAfter(NOTIFICATION_TIMEOUT_MS)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setWhen(System.currentTimeMillis());

        // 设置大图标（如果有）
        if (largeIcon != null) {
            builder.setLargeIcon(largeIcon);
        }

        return builder.build();
    }

    /**
     * 创建主PendingIntent
     */
    private PendingIntent createMainPendingIntent(String callerName, String callerNumber) {
        try {
            Intent mainIntent = new Intent(this, MainActivity.class);
            mainIntent.putExtra(EXTRA_CALLER_NAME, callerName);
            mainIntent.putExtra(EXTRA_CALLER_NUMBER, callerNumber);
            mainIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

            return PendingIntent.getActivity(this, generateRequestCode("main"),
                    mainIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        } catch (Exception e) {
            Log.e(TAG, "createMainPendingIntent: 创建主PendingIntent失败", e);
            return null;
        }
    }

    /**
     * 加载应用图标
     */
    private Bitmap loadAppIcon() {
        try {
            return BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);
        } catch (Exception e) {
            Log.w(TAG, "loadAppIcon: 加载应用图标失败", e);
            return null;
        }
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
                    wakeLock.acquire(WAKE_LOCK_TIMEOUT);
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
        try {
            if (notificationManager != null) {
                notificationManager.cancel(NOTIFICATION_ID_KEEP_ALIVE);
                notificationManager.cancel(NOTIFICATION_ID_CALL);
                Log.d(TAG, "cancelAllNotifications: 所有通知已取消");
            }
        } catch (Exception e) {
            Log.e(TAG, "cancelAllNotifications: 取消通知失败", e);
        }
    }

    /**
     * 生成请求码
     */
    private int generateRequestCode(String type) {
        return (type.hashCode() & 0xFFFF) + (int) (System.currentTimeMillis() & 0xFFFF);
    }

    /**
     * 联系人信息内部类
     */
    private static class ContactInfo {
        String contactName = "";
        String groupName = "";
    }
}