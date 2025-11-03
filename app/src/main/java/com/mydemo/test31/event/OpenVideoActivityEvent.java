package com.mydemo.test31.event;

import android.app.AlertDialog;

import com.mpttpnas.api.TrunkingCallSession;

public class OpenVideoActivityEvent {

    // 可以添加需要传递的数据

    public int callId;
    public TrunkingCallSession callSession;

    private AlertDialog alertDialog = null;

    public OpenVideoActivityEvent(int callId, TrunkingCallSession callSession) {
        this.callId = callId;
        this.callSession = callSession;
    }

    public int getCallId() {
        return this.callId;
    }

    public TrunkingCallSession getCallSession() {
        return this.callSession;
    }

    public AlertDialog getAlertDialog() {
        return alertDialog;
    }

    public void setAlertDialog(AlertDialog alertDialog) {
        this.alertDialog = alertDialog;
    }
}
