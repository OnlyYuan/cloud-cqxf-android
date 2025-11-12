package com.mydemo.test31.event;

import com.mpttpnas.api.TrunkingCallSession;

public class CallActionEvent {
    public static final int ACTION_ANSWER = 1;
    public static final int ACTION_REJECT = 2;

    public int callId;
    public TrunkingCallSession callSession;
    public int action;

    public CallActionEvent(int callId, TrunkingCallSession callSession, int action) {
        this.callId = callId;
        this.callSession = callSession;
        this.action = action;
    }
}