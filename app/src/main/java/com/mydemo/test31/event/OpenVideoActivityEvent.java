package com.mydemo.test31.event;

import com.mpttpnas.api.TrunkingCallSession;
import com.mydemo.test31.dialog.CallReminderDialog;

public class OpenVideoActivityEvent {

    // 可以添加需要传递的数据

    public int callId;
    public TrunkingCallSession callSession;

    private CallReminderDialog callReminderDialog = null;

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

    public CallReminderDialog getCallReminderDialog() {
        return callReminderDialog;
    }

    public void setCallReminderDialog(CallReminderDialog callReminderDialog) {
        this.callReminderDialog = callReminderDialog;
    }
}
