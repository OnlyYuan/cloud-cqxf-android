package com.xycm.cqxf.util;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.core.app.NotificationCompat;

import com.mpttpnas.api.TrunkingCallSession;
import com.xycm.cqxf.R;
import com.xycm.cqxf.call.receiver.CallReceiver;
import com.xycm.cqxf.service.MessageUiService;

public class NotificationHelper {

    private static final String CHANNEL_ID_KEEP_ALIVE = "keep_alive_channel";
    private static final String CHANNEL_ID_CALL = "incoming_call_channel";

    private static final String TAG = "NotificationHelper";

    private final NotificationManager notificationManager;
    private final Context context;

    // 通知ID
    private static final int NOTIFICATION_ID_KEEP_ALIVE = 1;
    public static final int NOTIFICATION_ID_CALL = 1002;

    private static final int REQ_ANSWER = 1001;
    private static final int REQ_REJECT = 1002;

    // Extra keys
    private static final String EXTRA_CALLER_NAME = "caller_name";
    private static final String EXTRA_CALLER_NUMBER = "caller_number";

    private static final String CHANNEL_NAME_KEEP_ALIVE = "保活服务";
    private static final String CHANNEL_NAME_CALL = "来电通知";
    private static final String CHANNEL_DESC_KEEP_ALIVE = "应用后台运行状态";
    private static final String CHANNEL_DESC_CALL = "来电通知提醒";

    private static final long NOTIFICATION_TIMEOUT_MS = 30000; // 30秒
    private static final long[] VIBRATION_PATTERN = {0, 500, 200, 500};

    public NotificationHelper(Context context) {
        this.context = context;
        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    public void showKeepAliveNotification(Context context) {
        Notification notification = new NotificationCompat.Builder(context, CHANNEL_ID_KEEP_ALIVE)
                .setContentTitle("保活服务")
                .setContentText("应用正在后台运行")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .build();
        notificationManager.notify(NOTIFICATION_ID_KEEP_ALIVE, notification);
    }

    public void showCallNotification(Context context, String callerName, String callerNumber) {
        RemoteViews remoteViews = createCallNotificationView(context, callerName, callerNumber);
        Notification notification = new NotificationCompat.Builder(context, CHANNEL_ID_CALL)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setCustomContentView(remoteViews)
                .setCustomBigContentView(remoteViews)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setOngoing(true)
                .build();
        notificationManager.notify(NOTIFICATION_ID_CALL, notification);
    }

    private RemoteViews createCallNotificationView(Context context, String callerName, String callerNumber) {
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.custom_call_notification);
        remoteViews.setTextViewText(R.id.notification_title, "来电");
        remoteViews.setTextViewText(R.id.notification_content, buildContentText(callerName, callerNumber));
        return remoteViews;
    }

    private String buildContentText(String callerName, String callerNumber) {
        if (!TextUtils.isEmpty(callerName) && !callerName.equals(callerNumber)) {
            return callerName + " 来电\n" + callerNumber;
        } else {
            return callerNumber + " 来电";
        }
    }

    public void cancelNotifications() {
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
     * 创建通知渠道
     */
    public void createNotificationChannels() {
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
    public void showCustomCallNotification(String callerName, String callerNumber,
                                           TrunkingCallSession callSession) {
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
            RemoteViews remoteViews = createNotificationView(callerName, callerNumber, callSession);
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
    public RemoteViews createNotificationView(String callerName, String callerNumber,
                                              TrunkingCallSession callSession) {
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.custom_call_notification);
        // 设置通知内容
        remoteViews.setTextViewText(R.id.notification_title, context.getString(R.string.incoming_call_title));
        String contentText = buildContentText(callerName, callerNumber);
        remoteViews.setTextViewText(R.id.notification_content, contentText);
        // 设置按钮点击事件
        setupButtonActions(remoteViews, callSession);
        return remoteViews;
    }

    /**
     * 构建通话通知
     */
    private Notification buildCallNotification(RemoteViews remoteViews, String callerName,
                                               String callerNumber) {
        PendingIntent mainPendingIntent = createMainPendingIntent(callerName, callerNumber);
        Bitmap largeIcon = loadAppIcon();
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID_CALL)
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
     * 设置按钮动作
     */
    private void setupButtonActions(RemoteViews remoteViews, TrunkingCallSession callSession) {
        try {
            // 接听按钮
            Intent answerIntent = CallReceiver.createAnswerIntent(context, callSession);
            PendingIntent answerPendingIntent = PendingIntent.getBroadcast(
                    context, REQ_ANSWER, answerIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            remoteViews.setOnClickPendingIntent(R.id.btn_accept, answerPendingIntent);

            // 拒绝按钮
            Intent rejectIntent = CallReceiver.createRejectIntent(context, callSession);
            PendingIntent rejectPendingIntent = PendingIntent.getBroadcast(
                    context, REQ_REJECT, rejectIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            remoteViews.setOnClickPendingIntent(R.id.btn_reject, rejectPendingIntent);
        } catch (Exception e) {
            Log.e(TAG, "setupButtonActions: 设置按钮动作失败", e);
        }
    }

    /**
     * 创建主PendingIntent
     */
    private PendingIntent createMainPendingIntent(String callerName, String callerNumber) {
        try {
            Intent mainIntent = new Intent(context, MessageUiService.class);
            mainIntent.putExtra(EXTRA_CALLER_NAME, callerName);
            mainIntent.putExtra(EXTRA_CALLER_NUMBER, callerNumber);
            mainIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            return PendingIntent.getActivity(context, 1000,
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
            return BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher);
        } catch (Exception e) {
            Log.w(TAG, "loadAppIcon: 加载应用图标失败", e);
            return null;
        }
    }
}
