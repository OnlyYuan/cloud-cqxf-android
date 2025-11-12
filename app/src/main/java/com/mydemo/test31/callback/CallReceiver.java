package com.mydemo.test31.callback;

import static com.mydemo.test31.service.KeepAliveService.NOTIFICATION_ID_CALL;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.mpttpnas.api.TrunkingCallSession;
import com.mpttpnas.pnaslibraryapi.PnasCallUtil;
import com.mydemo.test31.activity.MessageUiActivity;

public class CallReceiver extends BroadcastReceiver {
    private static final String TAG = "CallReceiver";

    public static final String ACTION_ANSWER_CALL = "com.mydemo.test31.ACTION_ANSWER_CALL";
    public static final String ACTION_REJECT_CALL = "com.mydemo.test31.ACTION_REJECT_CALL";

    // 额外参数键名
    private static final String EXTRA_CALL_SESSION = "callSession";
    private static final String EXTRA_CALLER_NUMBER = "callerNumber";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) {
            Log.w(TAG, "onReceive: intent is null");
            return;
        }

        String action = intent.getAction();
        TrunkingCallSession callSession = intent.getParcelableExtra(EXTRA_CALL_SESSION);

        if (callSession == null) {
            Log.e(TAG, "onReceive: callSession is null for action: " + action);
            cancelNotification(context);
            return;
        }

        Log.d(TAG, "onReceive: action=" + action + ", callId=" + callSession.callId);

        try {
            if (ACTION_ANSWER_CALL.equals(action)) {
                answerCall(context, callSession);
            } else if (ACTION_REJECT_CALL.equals(action)) {
                rejectCall(context, callSession);
            } else {
                Log.w(TAG, "onReceive: unknown action: " + action);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error handling call action: " + action, e);
        } finally {
            cancelNotification(context);
        }
    }

    private void answerCall(Context context, TrunkingCallSession callSession) {
        Log.i(TAG, "Answering call: " + callSession.callId);

        try {
            Intent intent = new Intent(context, MessageUiActivity.class);
            intent.putExtra("comeType", 1);
            intent.putExtra(EXTRA_CALL_SESSION, callSession);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                    Intent.FLAG_ACTIVITY_CLEAR_TOP |
                    Intent.FLAG_ACTIVITY_SINGLE_TOP);

            context.startActivity(intent);
            Log.d(TAG, "Started MessageUiActivity for call answering");
        } catch (Exception e) {
            Log.e(TAG, "Failed to start MessageUiActivity", e);
            // 可以在这里添加备用处理逻辑
        }
    }

    private void rejectCall(Context context, TrunkingCallSession callSession) {
        Log.i(TAG, "Rejecting call: " + callSession.callId);
        try {
            PnasCallUtil.getInstance().hangupActiveCall();
        } catch (Exception e) {
            Log.e(TAG, "Failed to post reject event to EventBus", e);
            // 可以在这里添加直接拒绝通话的逻辑
        }
    }

    private void cancelNotification(Context context) {
        try {
            NotificationManager notificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                notificationManager.cancel(NOTIFICATION_ID_CALL);
                Log.d(TAG, "Cancelled notification: " + NOTIFICATION_ID_CALL);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to cancel notification", e);
        }
    }

    /**
     * 创建接听意图的辅助方法
     */
    public static Intent createAnswerIntent(Context context, TrunkingCallSession callSession) {
        Intent intent = new Intent(context, CallReceiver.class);
        intent.setAction(ACTION_ANSWER_CALL);
        intent.putExtra(EXTRA_CALL_SESSION, callSession);
        return intent;
    }

    /**
     * 创建拒绝意图的辅助方法
     */
    public static Intent createRejectIntent(Context context, TrunkingCallSession callSession) {
        Intent intent = new Intent(context, CallReceiver.class);
        intent.setAction(ACTION_REJECT_CALL);
        intent.putExtra(EXTRA_CALL_SESSION, callSession);
        return intent;
    }
}