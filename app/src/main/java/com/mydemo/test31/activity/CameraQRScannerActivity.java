package com.mydemo.test31.activity;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.mydemo.test31.R;
import com.mydemo.test31.event.QRScannerEvent;

import org.greenrobot.eventbus.EventBus;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class CameraQRScannerActivity extends AppCompatActivity
        implements TextureView.SurfaceTextureListener, Camera.PreviewCallback {

    private static final String TAG = "CameraQRScanner";
    // 扫描间隔(ms)
    private static final int SCAN_INTERVAL = 500;

    private TextureView textureView;
    private View scanLine;
    private Button btnBack, btnFlash;
    private View scanFrame;

    private Camera camera;
    private boolean isFlashOn = false;
    private boolean isScanning = false;

    private MultiFormatReader multiFormatReader;
    private ScheduledExecutorService scanExecutor;
    private Handler mainHandler;

    private Vibrator vibrator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_qr);

        initViews();
        initScanner();

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        mainHandler = new Handler(Looper.getMainLooper());
    }

    private void initViews() {
        textureView = findViewById(R.id.textureView);
        scanLine = findViewById(R.id.scanLine);
        scanFrame = findViewById(R.id.scanFrame);
        btnBack = findViewById(R.id.btnBack);
        btnFlash = findViewById(R.id.btnFlash);

        textureView.setSurfaceTextureListener(this);

        btnBack.setOnClickListener(v -> finish());
        btnFlash.setOnClickListener(v -> toggleFlash());

        // 等视图加载完成后启动动画
        getWindow().getDecorView().post(this::startScanLineAnimation);
    }

    private void initScanner() {
        multiFormatReader = new MultiFormatReader();
    }

    private void startScanLineAnimation() {
        if (scanFrame == null || scanLine == null) {
            Log.e(TAG, "扫描框或扫描线视图未找到");
            return;
        }

        // 确保视图已经测量完成
        if (scanFrame.getHeight() == 0) {
            scanFrame.post(this::setupScanAnimation);
        } else {
            setupScanAnimation();
        }
    }

    private void setupScanAnimation() {
        ObjectAnimator animator = ObjectAnimator.ofFloat(
                scanLine,
                "translationY",
                -scanFrame.getHeight() / 2,
                scanFrame.getHeight() / 2
        );
        animator.setDuration(1800);
        animator.setRepeatCount(ObjectAnimator.INFINITE);
        animator.setRepeatMode(ObjectAnimator.REVERSE);
        animator.start();
    }

    private void toggleFlash() {
        if (camera == null) return;

        Camera.Parameters parameters = camera.getParameters();
        List<String> flashModes = parameters.getSupportedFlashModes();

        if (flashModes == null || flashModes.isEmpty()) {
            Toast.makeText(this, "该设备不支持闪光灯", Toast.LENGTH_SHORT).show();
            return;
        }

        String flashMode;
        if (isFlashOn) {
            flashMode = Camera.Parameters.FLASH_MODE_OFF;
            btnFlash.setText("闪光灯");
        } else {
            if (flashModes.contains(Camera.Parameters.FLASH_MODE_TORCH)) {
                flashMode = Camera.Parameters.FLASH_MODE_TORCH;
            } else {
                flashMode = Camera.Parameters.FLASH_MODE_ON;
            }
            btnFlash.setText("关闭闪光");
        }

        parameters.setFlashMode(flashMode);
        camera.setParameters(parameters);
        isFlashOn = !isFlashOn;
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        startCamera();
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        // 表面尺寸变化时重新配置相机
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        // 表面更新
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        stopCamera();
        return true;
    }

    private void startCamera() {
        try {
            // 找到后置摄像头
            int cameraId = findBackCamera();
            if (cameraId == -1) {
                Toast.makeText(this, "未找到后置摄像头", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            camera = Camera.open(cameraId);
            // 设置相机方向 - 修复画面歪斜的关键代码
            setCameraDisplayOrientation(cameraId);
            // 配置相机参数
            Camera.Parameters parameters = camera.getParameters();
            // 设置预览尺寸
            Camera.Size optimalSize = getOptimalPreviewSize(parameters.getSupportedPreviewSizes(),
                    textureView.getWidth(), textureView.getHeight());
            if (optimalSize != null) {
                parameters.setPreviewSize(optimalSize.width, optimalSize.height);
            } else {
                // 使用默认尺寸
                List<Camera.Size> sizes = parameters.getSupportedPreviewSizes();
                if (!sizes.isEmpty()) {
                    Camera.Size size = sizes.get(0);
                    parameters.setPreviewSize(size.width, size.height);
                }
            }

            // 设置对焦模式
            List<String> focusModes = parameters.getSupportedFocusModes();
            if (focusModes != null && focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            } else if (focusModes != null && focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            }

            // 设置图片格式
            parameters.setPreviewFormat(ImageFormat.NV21);

            camera.setParameters(parameters);
            camera.setPreviewTexture(textureView.getSurfaceTexture());
            camera.setPreviewCallback(this);
            camera.startPreview();

            // 开始定时扫描
            startPeriodicScan();

        } catch (Exception e) {
            Log.e(TAG, "启动相机失败", e);
            Toast.makeText(this, "启动相机失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    /**
     * 设置相机预览方向，确保画面不歪斜
     */
    private void setCameraDisplayOrientation(int cameraId) {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }
        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // 前置摄像头需要镜像
        } else {
            // 后置摄像头
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);
    }

    private int findBackCamera() {
        int numberOfCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                return i;
            }
        }
        // 如果没有找到后置摄像头，尝试使用默认摄像头（索引0）
        return numberOfCameras > 0 ? 0 : -1;
    }

    private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h) {
        if (sizes == null || w == 0 || h == 0) return null;

        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) w / h;

        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - h) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - h);
            }
        }

        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - h) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - h);
                }
            }
        }
        return optimalSize;
    }

    private void startPeriodicScan() {
        if (scanExecutor != null) {
            scanExecutor.shutdown();
        }

        scanExecutor = Executors.newSingleThreadScheduledExecutor();
        scanExecutor.scheduleAtFixedRate(() -> {
            if (!isScanning) {
                isScanning = true;
            }
        }, 0, SCAN_INTERVAL, TimeUnit.MILLISECONDS);
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if (!isScanning) return;

        Camera.Size previewSize = camera.getParameters().getPreviewSize();
        if (previewSize == null) return;

        int width = previewSize.width;
        int height = previewSize.height;

        // 只扫描中心区域，提高性能
        int scanWidth = width / 2;
        int scanHeight = height / 2;
        int startX = (width - scanWidth) / 2;
        int startY = (height - scanHeight) / 2;

        try {
            PlanarYUVLuminanceSource source = new PlanarYUVLuminanceSource(
                    data, width, height, startX, startY, scanWidth, scanHeight, false
            );

            BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(source));

            Result result = multiFormatReader.decodeWithState(binaryBitmap);
            if (result != null) {
                isScanning = false;
                mainHandler.post(() -> handleScanResult(result));
            }
        } catch (Exception e) {
            // 解码失败，继续扫描
        }
    }

    private void handleScanResult(Result result) {
        String content = result.getText();
        String format = result.getBarcodeFormat().toString();

        Log.d(TAG, "扫描结果: " + content + ", 格式: " + format);

        // 振动反馈
        if (vibrator != null && vibrator.hasVibrator()) {
            try {
                vibrator.vibrate(200);
            } catch (Exception e) {
                Log.e(TAG, "振动失败", e);
            }
        }

        QRScannerEvent qrScannerEvent = new QRScannerEvent(content);
        EventBus.getDefault().post(qrScannerEvent);

        // 显示结果并关闭
        Toast.makeText(this, "扫描成功!", Toast.LENGTH_SHORT).show();

        // 延迟关闭，让用户看到结果
        mainHandler.postDelayed(() -> finish(), 500);
    }

    private void stopCamera() {
        isScanning = false;

        if (scanExecutor != null) {
            scanExecutor.shutdown();
            try {
                if (!scanExecutor.awaitTermination(1, TimeUnit.SECONDS)) {
                    scanExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                scanExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
            scanExecutor = null;
        }

        if (camera != null) {
            try {
                camera.stopPreview();
                camera.setPreviewCallback(null);
                camera.release();
            } catch (Exception e) {
                Log.e(TAG, "停止相机失败", e);
            }
            camera = null;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopCamera();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (textureView.isAvailable() && camera == null) {
            startCamera();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopCamera();

        if (mainHandler != null) {
            mainHandler.removeCallbacksAndMessages(null);
        }
    }
}