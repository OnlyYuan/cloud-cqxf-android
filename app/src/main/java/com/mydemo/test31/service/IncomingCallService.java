package com.mydemo.test31.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * 来电服务
 */
public class IncomingCallService extends Service {

    private static final String TAG = "IncomingCallService";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


}
