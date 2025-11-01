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

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.e(THIS_FILE, "set surface: " + holder.getSurface() + ", type: " + type);
//        PnasCallUtil.getInstance().setSurface(holder.getSurface(), type);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.e(THIS_FILE, "surfaceChanged width: " + width + ", height: " + height);
        PnasCallUtil.getInstance().surfaceChanged(type, holder.getSurface(), width, height);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.e(THIS_FILE, "surfaceDestroyed");
        PnasCallUtil.getInstance().surfaceDestroy(type);
    }
}
