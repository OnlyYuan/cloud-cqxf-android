package com.mydemo.test31.util;

import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.util.DisplayMetrics;
import android.util.Log;

/**
 * 状态栏
 */
public class StatusBarPixelUtils {

    /**
     * 直接获取状态栏高度的像素值
     */
    public static int getStatusBarHeightPx(Context context) {
        int statusBarHeightPx = 0;

        try {
            // 方法1：通过系统dimension资源获取（最准确）
            int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
            if (resourceId > 0) {
                statusBarHeightPx = context.getResources().getDimensionPixelSize(resourceId);
                Log.d("StatusBarHeight", "通过资源获取状态栏高度: " + statusBarHeightPx + "px");
            }

            // 方法2：如果资源获取失败，通过应用区域计算
            if (statusBarHeightPx == 0 && context instanceof Activity) {
                Activity activity = (Activity) context;
                Rect rect = new Rect();
                activity.getWindow().getDecorView().getWindowVisibleDisplayFrame(rect);
                statusBarHeightPx = rect.top;
                Log.d("StatusBarHeight", "通过区域计算状态栏高度: " + statusBarHeightPx + "px");
            }

            // 方法3：保底方案，根据不同DPI提供默认像素值
            if (statusBarHeightPx == 0) {
                statusBarHeightPx = getDefaultStatusBarHeightPx(context);
                Log.d("StatusBarHeight", "使用默认状态栏高度: " + statusBarHeightPx + "px");
            }

        } catch (Exception e) {
            e.printStackTrace();
            statusBarHeightPx = getDefaultStatusBarHeightPx(context);
        }

        return statusBarHeightPx;
    }

    /**
     * 根据不同屏幕密度提供默认像素值
     */
    private static int getDefaultStatusBarHeightPx(Context context) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        float density = metrics.density;

        // 根据不同DPI设置默认像素值
        if (density >= 4.0) {
            return 112; // xxxhdpi: 28dp * 4 = 112px
        } else if (density >= 3.0) {
            return 84;  // xxhdpi: 28dp * 3 = 84px
        } else if (density >= 2.0) {
            return 56;  // xhdpi: 28dp * 2 = 56px
        } else if (density >= 1.5) {
            return 42;  // hdpi: 28dp * 1.5 = 42px
        } else {
            return 28;  // mdpi: 28dp * 1 = 28px
        }
    }

    /**
     * 实时获取当前状态栏高度（像素）
     */
    public static int getCurrentStatusBarHeightPx(Activity activity) {
        Rect rect = new Rect();
        activity.getWindow().getDecorView().getWindowVisibleDisplayFrame(rect);
        return rect.top;
    }
}