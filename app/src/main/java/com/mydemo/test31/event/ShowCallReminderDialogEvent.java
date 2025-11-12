package com.mydemo.test31.event;

import com.mpttpnas.api.TrunkingCallSession;

public class ShowCallReminderDialogEvent {

    // 可以添加需要传递的数据
    public int callId;
    public TrunkingCallSession callSession;


    public ShowCallReminderDialogEvent(int callId, TrunkingCallSession callSession) {
        this.callId = callId;
        this.callSession = callSession;
    }

    public int getCallId() {
        return this.callId;
    }

    public TrunkingCallSession getCallSession() {
        return this.callSession;
    }

}
