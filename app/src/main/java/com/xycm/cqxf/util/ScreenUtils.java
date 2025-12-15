package com.xycm.cqxf.util;

import android.app.Activity;
import android.content.pm.ActivityInfo;

public class ScreenUtils {

    /**
     * 设置屏幕方向
     *
     * @param activity Activity实例
     * @param orientation 屏幕方向（例如：SCREEN_ORIENTATION_PORTRAIT、SCREEN_ORIENTATION_LANDSCAPE等）
     */
    public static void setScreenOrientation(Activity activity, int orientation) {
        if (activity != null) {
            activity.setRequestedOrientation(orientation);
        }
    }
}
