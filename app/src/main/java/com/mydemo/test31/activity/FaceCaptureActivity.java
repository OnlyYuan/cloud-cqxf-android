// FaceCaptureActivity.java
package com.mydemo.test31.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.hardware.Camera;
import android.media.MediaActionSound;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mydemo.test31.R;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class FaceCaptureActivity extends AppCompatActivity implements
        SurfaceHolder.Callback,
        Camera.PreviewCallback,
        Camera.PictureCallback {

    // UI组件
    private SurfaceView surfaceView;
    private TextView titleTextView;
    private TextView countdownTextView;
    private Button captureButton;
    private ImageView closeButton;

    // 相机相关
    private Camera camera;
    private SurfaceHolder surfaceHolder;
    private int currentCameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;
    private boolean isCameraFrontFacing = true;
    private Camera.Size previewSize;
    private int previewWidth = 0;
    private int previewHeight = 0;

    // 状态控制
    private boolean isCapturing = false;
    private boolean isPreviewing = false;
    private boolean isProcessing = false;

    // 工具类
    private MediaActionSound mediaSound;
    private GestureDetector gestureDetector;
    private ScaleGestureDetector scaleGestureDetector;
    private Handler mainHandler;
    private CountDownTimer countdownTimer;
    private final Gson gson = new Gson();

    // 配置参数
    private Map<String, Object> config = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_capture);

        // 保持屏幕常亮
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // 设置沉浸式状态栏
        setupImmersiveMode();

        // 初始化UI
        initUI();

        // 初始化工具
        mediaSound = new MediaActionSound();
        mainHandler = new Handler(Looper.getMainLooper());

        // 获取配置
        loadConfig();

        // 初始化手势检测
        initGestureDetectors();
    }

    private void setupImmersiveMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                            View.SYSTEM_UI_FLAG_FULLSCREEN |
                            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

            // 设置状态栏透明
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                getWindow().setStatusBarColor(Color.TRANSPARENT);
            }
        }
    }

    private void initUI() {
        surfaceView = findViewById(R.id.surfaceView);
        titleTextView = findViewById(R.id.titleTextView);
        countdownTextView = findViewById(R.id.countdownTextView);
        captureButton = findViewById(R.id.captureButton);
        closeButton = findViewById(R.id.close_btn);

        // 设置关闭按钮监听器
        closeButton.setOnClickListener(v -> {
            if (isProcessing) {
                Toast.makeText(this, "正在处理图片，请稍候...", Toast.LENGTH_SHORT).show();
                return;
            }
            cancelCapture();
        });

        // 设置拍照按钮监听器
        captureButton.setOnClickListener(v -> {
            if (!isCapturing && !isProcessing) {
                capturePhoto();
            }
        });

        // 初始化SurfaceHolder
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    private void loadConfig() {
        String configJson = getIntent().getStringExtra("config");
        if (configJson != null) {
            Type type = new TypeToken<Map<String, Object>>() {}.getType();
            config = gson.fromJson(configJson, type);
        } else {
            // 默认配置
            config.put("quality", 0.85);
            config.put("mode", "photo");
            config.put("timeout", 30000L);
            config.put("needSound", true);
            config.put("maxSize", 1024);
            config.put("needBase64", true);
            config.put("autoCapture", false);
            config.put("countdown", 3);
        }

        // 设置引导文本
        updateGuideText("请保证光线充足，面容整洁的情况下进行人脸识别");
    }

    private void initGestureDetectors() {
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                // 双击切换摄像头
                switchCamera();
                return true;
            }

            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                // 单点对焦
                if (camera != null && isPreviewing) {
                    performAutoFocus(e);
                }
                return true;
            }
        });

        scaleGestureDetector = new ScaleGestureDetector(this,
                new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    @Override
                    public boolean onScale(ScaleGestureDetector detector) {
                        if (camera != null) {
                            adjustZoom(detector.getScaleFactor());
                        }
                        return true;
                    }
                });

        // 设置触摸监听器
        surfaceView.setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);
            scaleGestureDetector.onTouchEvent(event);
            return true;
        });
    }

    private void performAutoFocus(MotionEvent event) {
        Camera.Parameters params = camera.getParameters();
        if (params.getMaxNumFocusAreas() > 0) {
            Rect focusRect = calculateTapArea(event.getX(), event.getY(), 1f);
            List<Camera.Area> focusAreas = new java.util.ArrayList<>();
            focusAreas.add(new Camera.Area(focusRect, 1000));

            try {
                params.setFocusAreas(focusAreas);
                camera.setParameters(params);
                camera.autoFocus((success, cam) -> {
                    // 对焦完成回调
                });
            } catch (Exception ex) {
                Log.e("FaceCapture", "对焦失败: " + ex.getMessage());
            }
        }
    }

    private void adjustZoom(float scaleFactor) {
        Camera.Parameters params = camera.getParameters();
        if (params.isZoomSupported()) {
            int currentZoom = params.getZoom();
            int maxZoom = params.getMaxZoom();
            float newZoom = currentZoom * scaleFactor;
            newZoom = Math.max(1.0f, Math.min(newZoom, maxZoom));

            params.setZoom((int) newZoom);
            camera.setParameters(params);
        }
    }

    private Rect calculateTapArea(float x, float y, float coefficient) {
        int areaSize = Float.valueOf(200 * coefficient).intValue();

        int left = (int) (x - areaSize / 2);
        int top = (int) (y - areaSize / 2);
        int right = (int) (x + areaSize / 2);
        int bottom = (int) (y + areaSize / 2);

        // 确保在预览区域内
        left = Math.max(0, Math.min(left, previewWidth));
        top = Math.max(0, Math.min(top, previewHeight));
        right = Math.max(0, Math.min(right, previewWidth));
        bottom = Math.max(0, Math.min(bottom, previewHeight));

        return new Rect(left, top, right, bottom);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.d("FaceCapture", "Surface created");
        startCamera();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.d("FaceCapture", "Surface changed: " + width + "x" + height);
        if (camera != null && isPreviewing) {
            restartPreview();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d("FaceCapture", "Surface destroyed");
        releaseCamera();
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        // 可以在这里添加人脸检测逻辑
    }

    @Override
    public void onPictureTaken(byte[] data, Camera camera) {
        Log.d("FaceCapture", "Picture taken, data size: " + (data != null ? data.length : 0));
        isProcessing = true;

        // 在后台线程处理图片
        new Thread(() -> processImageData(data)).start();
    }

    private void processImageData(byte[] data) {
        try {
            // 播放拍照声音
            if (config.containsKey("needSound") && (Boolean) config.get("needSound")) {
                mediaSound.play(MediaActionSound.SHUTTER_CLICK);
            }

            // 解码图片数据
            Bitmap originalBitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
            if (originalBitmap == null) {
                throw new IOException("无法解码图片数据");
            }

            // 根据摄像头方向旋转图片
            Bitmap rotatedBitmap = rotateBitmap(originalBitmap);
            // 压缩图片
            Bitmap finalBitmap = compressBitmap(rotatedBitmap);
            // 保存到文件
            String filePath = saveBitmapToFile(finalBitmap);
            // 生成Base64字符串
            String base64Image = bitmapToBase64(finalBitmap);

            // 准备返回结果
            Map<String, Object> result = prepareResultData(finalBitmap, base64Image, filePath);

            // 回到主线程发送结果
            mainHandler.post(() -> sendResultAndFinish(result));

            // 释放Bitmap内存
            recycleBitmaps(originalBitmap, rotatedBitmap, finalBitmap);

        } catch (Exception e) {
            Log.e("FaceCapture", "处理图片失败: " + e.getMessage(), e);
            mainHandler.post(() -> {
                Toast.makeText(this, "处理图片失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                sendErrorResult("处理图片失败: " + e.getMessage());
            });
        } finally {
            isProcessing = false;
            isCapturing = false;

            // 重新开始预览
            mainHandler.post(() -> {
                if (camera != null) {
                    camera.startPreview();
                    isPreviewing = true;
                }
            });
        }
    }

    private Map<String, Object> prepareResultData(Bitmap bitmap, String base64Image, String filePath) {
        Map<String, Object> result = new HashMap<>();
        result.put("action", "capture");
        result.put("base64Image", base64Image);
        result.put("filePath", filePath);
        result.put("timestamp", System.currentTimeMillis());
        result.put("imageWidth", bitmap.getWidth());
        result.put("imageHeight", bitmap.getHeight());
        result.put("quality", config.get("quality"));
        result.put("cameraFacing", isCameraFrontFacing ? "front" : "back");
        return result;
    }

    private void sendResultAndFinish(Map<String, Object> result) {
        Intent resultIntent = new Intent();
        resultIntent.putExtra("action", "capture");
        resultIntent.putExtra("base64Image", (String) result.get("base64Image"));
        resultIntent.putExtra("filePath", (String) result.get("filePath"));
        resultIntent.putExtra("timestamp", String.valueOf(result.get("timestamp")));
        resultIntent.putExtra("imageWidth", (Integer) result.get("imageWidth"));
        resultIntent.putExtra("imageHeight", (Integer) result.get("imageHeight"));
        resultIntent.putExtra("quality", (Double) config.get("quality"));
        resultIntent.putExtra("cameraFacing", isCameraFrontFacing ? "front" : "back");
        setResult(RESULT_OK, resultIntent);
        finish();
    }

    private void sendErrorResult(String errorMessage) {
        Intent resultIntent = new Intent();
        resultIntent.putExtra("error", errorMessage);
        setResult(RESULT_CANCELED, resultIntent);
        finish();
    }

    private void recycleBitmaps(Bitmap... bitmaps) {
        for (Bitmap bitmap : bitmaps) {
            if (bitmap != null && !bitmap.isRecycled()) {
                bitmap.recycle();
            }
        }
    }

    private void startCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    200);
            return;
        }

        try {
            // 释放之前的相机
            releaseCamera();

            // 打开相机
            camera = Camera.open(currentCameraId);

            // 获取相机参数
            Camera.Parameters parameters = camera.getParameters();

            // 设置预览尺寸
            previewSize = getOptimalPreviewSize(parameters.getSupportedPreviewSizes(),
                    surfaceView.getWidth(), surfaceView.getHeight());
            parameters.setPreviewSize(previewSize.width, previewSize.height);
            previewWidth = previewSize.width;
            previewHeight = previewSize.height;

            // 设置图片尺寸
            Camera.Size pictureSize = getOptimalPictureSize(
                    parameters.getSupportedPictureSizes());
            parameters.setPictureSize(pictureSize.width, pictureSize.height);

            // 设置对焦模式
            List<String> focusModes = parameters.getSupportedFocusModes();
            if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            } else if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            }

            // 设置图片格式和质量
            parameters.setPictureFormat(ImageFormat.JPEG);
            parameters.setJpegQuality((int) (((Number) Objects.requireNonNull(config.get("quality"))).floatValue() * 100));

            // 应用参数
            camera.setParameters(parameters);

            // 设置显示方向
            setCameraDisplayOrientation();

            // 设置预览显示
            camera.setPreviewDisplay(surfaceHolder);
            camera.setPreviewCallback(this);

            // 开始预览
            camera.startPreview();
            isPreviewing = true;

            Log.d("FaceCapture", "Camera started: " + previewWidth + "x" + previewHeight);

        } catch (Exception e) {
            Log.e("FaceCapture", "启动相机失败: " + e.getMessage(), e);
            showErrorAndExit("无法启动相机: " + e.getMessage());
        }
    }

    private void restartPreview() {
        stopPreview();
        startPreview();
    }

    private void startPreview() {
        if (camera != null && !isPreviewing) {
            try {
                camera.startPreview();
                isPreviewing = true;
            } catch (Exception e) {
                Log.e("FaceCapture", "开始预览失败: " + e.getMessage());
            }
        }
    }

    private void stopPreview() {
        if (camera != null && isPreviewing) {
            try {
                camera.stopPreview();
                isPreviewing = false;
            } catch (Exception e) {
                Log.e("FaceCapture", "停止预览失败: " + e.getMessage());
            }
        }
    }

    private void releaseCamera() {
        if (camera != null) {
            try {
                camera.stopPreview();
                camera.setPreviewCallback(null);
                camera.release();
                camera = null;
                isPreviewing = false;
            } catch (Exception e) {
                Log.e("FaceCapture", "释放相机失败: " + e.getMessage());
            }
        }
    }

    private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int width, int height) {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) width / height;

        if (sizes == null) return null;

        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - height) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - height);
            }
        }

        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - height) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - height);
                }
            }
        }

        return optimalSize;
    }

    private Camera.Size getOptimalPictureSize(List<Camera.Size> sizes) {
        // 优先选择接近1080p的尺寸
        final int TARGET_WIDTH = 1920;
        final int TARGET_HEIGHT = 1080;

        if (sizes == null) return null;

        Camera.Size optimalSize = null;
        int minDiff = Integer.MAX_VALUE;

        for (Camera.Size size : sizes) {
            // 跳过太大或太小的尺寸
            if (size.width < 640 || size.height < 480) continue;

            int diff = Math.abs(size.width - TARGET_WIDTH) +
                    Math.abs(size.height - TARGET_HEIGHT);
            if (diff < minDiff) {
                optimalSize = size;
                minDiff = diff;
            }
        }

        // 如果没有找到合适的，选择最大的
        if (optimalSize == null && !sizes.isEmpty()) {
            optimalSize = sizes.get(0);
            for (Camera.Size size : sizes) {
                if (size.width * size.height > optimalSize.width * optimalSize.height) {
                    optimalSize = size;
                }
            }
        }

        return optimalSize;
    }

    private void setCameraDisplayOrientation() {
        if (camera == null) return;

        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(currentCameraId, info);

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
            result = (360 - result) % 360; // 镜像
        } else {
            result = (info.orientation - degrees + 360) % 360;
        }

        camera.setDisplayOrientation(result);
    }

    private void capturePhoto() {
        if (camera == null || isCapturing || isProcessing) {
            return;
        }

        isCapturing = true;

        // 显示倒计时
        if (config.containsKey("countdown") && ((Number) config.get("countdown")).intValue() > 0) {
            startCountdown(((Number) config.get("countdown")).intValue());
        } else {
            // 直接拍照
            takePicture();
        }
    }

    private void startCountdown(int seconds) {
        if (countdownTimer != null) {
            countdownTimer.cancel();
        }

        countdownTextView.setVisibility(View.VISIBLE);

        countdownTimer = new CountDownTimer(seconds * 1000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int secondsLeft = (int) (millisUntilFinished / 1000) + 1;
                countdownTextView.setText(String.valueOf(secondsLeft));

                // 最后3秒改变颜色
                if (secondsLeft <= 3) {
                    countdownTextView.setTextColor(Color.RED);
                } else {
                    countdownTextView.setTextColor(Color.WHITE);
                }
            }

            @Override
            public void onFinish() {
                countdownTextView.setVisibility(View.GONE);
                takePicture();
            }
        }.start();
    }

    private void takePicture() {
        if (camera == null) return;

        try {
            camera.takePicture(null, null, this);
        } catch (Exception e) {
            Log.e("FaceCapture", "拍照失败: " + e.getMessage(), e);
            isCapturing = false;
            Toast.makeText(this, "拍照失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void switchCamera() {
        if (isCapturing || isProcessing) return;

        int cameraCount = Camera.getNumberOfCameras();
        if (cameraCount < 2) {
            Toast.makeText(this, "未找到第二个摄像头", Toast.LENGTH_SHORT).show();
            return;
        }

        // 切换摄像头ID
        currentCameraId = (currentCameraId + 1) % cameraCount;
        isCameraFrontFacing = (currentCameraId == Camera.CameraInfo.CAMERA_FACING_FRONT);

        // 停止当前预览
        stopPreview();

        // 重新启动相机
        mainHandler.postDelayed(() -> {
            startCamera();
            updateGuideText(isCameraFrontFacing ?
                    "请将人脸对准框内" : "请将身份证对准框内");
        }, 300);
    }

    private void updateGuideText(String text) {
        if (titleTextView != null) {
            titleTextView.setText(text);
        }
    }

    private Bitmap rotateBitmap(Bitmap bitmap) {
        if (bitmap == null) return null;

        Matrix matrix = new Matrix();

        // 根据摄像头方向旋转
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(currentCameraId, info);

        int rotation = 0;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            rotation = 270; // 前置摄像头需要额外旋转
        } else {
            rotation = 90; // 后置摄像头
        }

        matrix.postRotate(rotation);

        // 如果是前置摄像头，需要镜像
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            matrix.postScale(-1, 1); // 水平镜像
        }

        return Bitmap.createBitmap(bitmap, 0, 0,
                bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    private Bitmap compressBitmap(Bitmap bitmap) {
        if (bitmap == null) return null;

        int maxSize = 1024; // 默认1024px
        if (config.containsKey("maxSize")) {
            maxSize = ((Number) config.get("maxSize")).intValue();
        }

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        if (width <= maxSize && height <= maxSize) {
            return bitmap;
        }

        float scale = Math.min((float) maxSize / width, (float) maxSize / height);
        int newWidth = Math.round(width * scale);
        int newHeight = Math.round(height * scale);

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
    }

    private String bitmapToBase64(Bitmap bitmap) {
        if (bitmap == null) return "";

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int quality = (int) (((Number) config.get("quality")).floatValue() * 100);
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos);
        byte[] byteArray = baos.toByteArray();

        return Base64.encodeToString(byteArray, Base64.NO_WRAP);
    }

    private String saveBitmapToFile(Bitmap bitmap) {
        // 检查存储权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    201);
            return "";
        }

        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            return "";
        }

        try {
            // 创建应用专属目录
            File storageDir = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "FaceCapture");

            if (!storageDir.exists()) {
                if (!storageDir.mkdirs()) {
                    Log.e("FaceCapture", "创建目录失败: " + storageDir.getAbsolutePath());
                    return "";
                }
            }

            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                    .format(new Date());
            String fileName = "FACE_" + timeStamp + ".jpg";
            File file = new File(storageDir, fileName);

            FileOutputStream fos = new FileOutputStream(file);
            int quality = (int) (((Number) config.get("quality")).floatValue() * 100);
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, fos);
            fos.close();

            Log.d("FaceCapture", "图片保存成功: " + file.getAbsolutePath());
            return file.getAbsolutePath();
        } catch (Exception e) {
            Log.e("FaceCapture", "保存文件失败: " + e.getMessage());
            return "";
        }
    }

    private void cancelCapture() {
        if (countdownTimer != null) {
            countdownTimer.cancel();
            countdownTextView.setVisibility(View.GONE);
        }

        if (isProcessing) {
            Toast.makeText(this, "正在处理中，请稍候...", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent resultIntent = new Intent();
        resultIntent.putExtra("action", "cancel");
        setResult(RESULT_CANCELED, resultIntent);
        finish();
    }

    private void showErrorAndExit(String message) {
        if (isFinishing() || isDestroyed()) {
            Log.d("FaceCapture", "Activity is finishing, skip dialog");
            return;
        }

        runOnUiThread(() -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }

            try {
                new AlertDialog.Builder(FaceCaptureActivity.this)
                        .setTitle("错误")
                        .setMessage(message)
                        .setPositiveButton("确定", (dialog, which) -> {
                            dialog.dismiss();
                            sendErrorResult(message);
                        })
                        .setCancelable(false)
                        .show();
            } catch (Exception e) {
                Log.e("FaceCapture", "显示对话框失败: " + e.getMessage());
                sendErrorResult(message);
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 200) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                showErrorAndExit("需要摄像头权限才能使用人脸识别功能");
            }
        } else if (requestCode == 201) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 存储权限已授予
            } else {
                Toast.makeText(this, "需要存储权限保存照片", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (camera == null) {
            startCamera();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (countdownTimer != null) {
            countdownTimer.cancel();
        }
        stopPreview();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countdownTimer != null) {
            countdownTimer.cancel();
        }
        releaseCamera();

        if (mediaSound != null) {
            mediaSound.release();
        }
    }
}