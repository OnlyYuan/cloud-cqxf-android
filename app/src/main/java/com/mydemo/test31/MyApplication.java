package com.mydemo.test31;

import android.app.Application;
import android.util.Log;

import com.mpttpnas.pnaslibraryapi.PnasApplicationUtil;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Log.e("MyApplication", "onCreate() called");
        PnasApplicationUtil.getInstance().initApplication(this);
    }
}
