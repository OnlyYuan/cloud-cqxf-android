package com.mydemo.test31;

import android.util.Log;
import android.view.SurfaceHolder;

import com.mptt.media.api.MediaEngineApiUtil;
import com.mpttpnas.pnaslibraryapi.PnasCallUtil;

public class VideoSurfaceCallback implements SurfaceHolder.Callback {

    private final String THIS_FILE = "VideoSurfaceCallback";

    private int type = MediaEngineApiUtil.LAUNCH_LOCAL_VIDEO;

    public VideoSurfaceCallback(int type) {
        this.type = type;
    }

    /**
     * 设置视频窗口
     */
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.e(THIS_FILE, "set surface: " + holder.getSurface() + ", type: " + type);
        PnasCallUtil.getInstance().setSurface(holder.getSurface(), type);
    }

    /**
     * 视频窗口变化
     */
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.e(THIS_FILE, "surfaceChanged width: " + width + ", height: " + height);
        PnasCallUtil.getInstance().surfaceChanged(type, holder.getSurface(), width, height);
    }

    /**
     * 视频窗口销毁
     */
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.e(THIS_FILE, "surfaceDestroyed");
        PnasCallUtil.getInstance().surfaceDestroy(type);
    }
}
