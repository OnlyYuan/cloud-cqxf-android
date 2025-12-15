package com.xycm.cqxf.call.manager;

import android.content.Context;
import android.text.TextUtils;

import com.mpttpnas.api.TrunkingCallSession;
import com.xycm.cqxf.call.event.ShowCallReminderDialogEvent;
import com.xycm.cqxf.util.NotificationHelper;

import org.greenrobot.eventbus.EventBus;

/**
 * 通话管理器（包括状态处理等）
 */
public class CallManager {

    private static final String TAG = "CallManager";

    private TrunkingCallSession callSession;
    private final NotificationHelper notificationHelper;
    private final Context context;

    public CallManager(Context context, NotificationHelper notificationHelper) {
        this.context = context;
        this.notificationHelper = notificationHelper;
    }

    public void handleCallStateChange(TrunkingCallSession session) {
        this.callSession = session;

        if (callSession == null) return;

        if (callSession.isIncoming()) {
            if (callSession.isBeforeConfirmed()) {
                // 早期媒体/振铃中，显示来电通知
                handleIncomingCall();
            }
        }

        if (callSession.isAfterEnded()) {
            // 通话结束后取消通知
            notificationHelper.cancelNotifications();
        }
    }

    private void handleIncomingCall() {
        // 处理来电状态，显示来电通知
        String callerName = TextUtils.isEmpty(callSession.getRemoteContact()) ? "未知联系人" : callSession.getRemoteContact();
        // 需要优化成号码
        String callerNumber = callerName;
        notificationHelper.showCallNotification(context, callerName, callerNumber);

        // 发送来电提醒对话框事件
        ShowCallReminderDialogEvent event = new ShowCallReminderDialogEvent(callSession.getCallId(), callSession);
        EventBus.getDefault().post(event);
    }
}
