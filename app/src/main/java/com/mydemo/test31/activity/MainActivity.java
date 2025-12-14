package com.mydemo.test31.activity;

import static com.mydemo.test31.util.Util.pocUrl;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.ConsoleMessage;
import android.webkit.JavascriptInterface;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mptt.media.modules.uvc.utils.ToastUtils;
import com.mpttpnas.api.TrunkingCallSession;
import com.mpttpnas.api.TrunkingConversation;
import com.mpttpnas.api.TrunkingGroupContact;
import com.mpttpnas.api.TrunkingMessage;
import com.mpttpnas.api.TrunkingProfileState;
import com.mpttpnas.pnas.agent.PnasErrorCode;
import com.mpttpnas.pnaslibraryapi.PnasCallUtil;
import com.mpttpnas.pnaslibraryapi.PnasConfigUtil;
import com.mpttpnas.pnaslibraryapi.PnasGisUtil;
import com.mpttpnas.pnaslibraryapi.PnasSDK;
import com.mpttpnas.pnaslibraryapi.PnasUserUtil;
import com.mpttpnas.pnaslibraryapi.callback.FloorStateChangedCallbackEvent;
import com.mpttpnas.pnaslibraryapi.callback.GroupAffiliactionNotifyResultCallbackEvent;
import com.mpttpnas.pnaslibraryapi.callback.RegistrationStateChangedCallbackEvent;
import com.mpttpnas.pnaslibraryapi.callback.StackStartSuccessCallbackEvent;
import com.mpttpnas.pnaslibraryapi.callback.StandbyGroupInfoChangedCallbackEvent;
import com.mydemo.test31.MyApplication;
import com.mydemo.test31.R;
import com.mydemo.test31.data.User;
import com.mydemo.test31.dialog.CallReminderDialog;
import com.mydemo.test31.dialog.LinkWayDialog;
import com.mydemo.test31.dialog.MemberListDialog;
import com.mydemo.test31.dialog.SelectPicDialog;
import com.mydemo.test31.dialog.UnitListDialog;
import com.mydemo.test31.event.CloseVideoActivityEvent;
import com.mydemo.test31.event.OpenVideoActivityEvent;
import com.mydemo.test31.event.QRScannerEvent;
import com.mydemo.test31.event.ShowCallReminderDialogEvent;
import com.mydemo.test31.service.KeepAliveService;
import com.mydemo.test31.util.AndroidVersionUtils;
import com.mydemo.test31.util.DatabaseManager;
import com.mydemo.test31.util.FileUploader;
import com.mydemo.test31.util.InvState;
import com.mydemo.test31.util.Util;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;

public class MainActivity extends AppCompatActivity implements View.OnTouchListener, View.OnClickListener {
    private static final String TAG = "MainActivity";

    public final static String PERMISSION_ACCESS_COARSE_LOCATION = "android.permission.ACCESS_COARSE_LOCATION";
    public final static String PERMISSION_ACCESS_FINE_LOCATION = "android.permission.ACCESS_FINE_LOCATION";
    public final static String PERMISSION_CAMERA = "android.permission.CAMERA";
    public final static String PERMISSION_CALL_PHONE = "android.permission.CALL_PHONE";
    public final static String PERMISSION_GET_ACCOUNTS = "android.permission.GET_ACCOUNTS";
    public final static String PERMISSION_PROCESS_OUTGOING_CALLS = "android.permission.PROCESS_OUTGOING_CALLS";
    public final static String PERMISSION_RECORD_AUDIO = "android.permission.RECORD_AUDIO";
    public final static String PERMISSION_READ_PHONE_STATE = "android.permission.READ_PHONE_STATE";
    public final static String PERMISSION_WRITE_CALL_LOG = "android.permission.WRITE_CALL_LOG";
    public final static String PERMISSION_WRITE_EXTERNAL_STORAGE = "android.permission.WRITE_EXTERNAL_STORAGE";
    public final static String PERMISSION_READ_EXTERNAL_STORAGE = "android.permission.READ_EXTERNAL_STORAGE";
    public final static String PERMISSION_CHANGE_NETWORK_STATE = "android.permission.CHANGE_NETWORK_STATE";
    public final static String PERMISSION_WRITE_SETTINGS = "android.permission.WRITE_SETTINGS";
    public final static String PERMISSION_POST_NOTIFICATIONS = "android.permission.POST_NOTIFICATIONS";
    public final static String PERMISSION_AUDIO_SETTINGS = "android.permission.MODIFY_AUDIO_SETTINGS";
    public final static String PERMISSION_READ_MEDIA_IMAGES = "android.permission.READ_MEDIA_IMAGES";
    public final static String PERMISSION_MOUNT_UNMOUNT_FILESYSTEMS = "android.permission.MOUNT_UNMOUNT_FILESYSTEMS";

    public final static String[] permissionNeedToCheck = {PERMISSION_READ_PHONE_STATE,           // 读取手机状态
            PERMISSION_ACCESS_FINE_LOCATION,       // 精确位置
            PERMISSION_ACCESS_COARSE_LOCATION,     // 粗略位置
            PERMISSION_CAMERA,                     // 相机
            PERMISSION_RECORD_AUDIO,               // 录音
            PERMISSION_READ_MEDIA_IMAGES, PERMISSION_READ_EXTERNAL_STORAGE,      // 读取外部存储
            PERMISSION_WRITE_EXTERNAL_STORAGE,     // 写入外部存储
            PERMISSION_POST_NOTIFICATIONS,         // 发送通知 (Android 13+)
            PERMISSION_AUDIO_SETTINGS              // 音频设置
    };

    public static boolean isGrantPermissions(Activity activity, List<String> permissionList) {
        if (activity == null || permissionList.isEmpty()) {
            return false;
        }
        for (int i = 0; i < permissionList.size(); i++) {
            if (activity.checkSelfPermission(permissionList.get(i)) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private WebView webView;

    // 建立连接方式 0.语音  1.视频
    private int callType = 0;

    // 判断poc是否初始化完成
    private boolean isInitPnasUserUtilSuccess = false;
    // h5是否调用了登录
    private boolean isH5Login = false;

    // h5传入的用户名
    private String useName = "";

    // h5传入的密码
    private String passWord = "";

    // 用于向H5返回选择结果
    private ValueCallback<Uri[]> filePathCallback;
    private static final int FILE_CHOOSER_REQUEST_CODE = 1001;
    private static final int REQUEST_TAKE_PHOTO = 1002;

    /**
     * 人脸识别权限
     */
    private static final int FACE_CAPTURE_REQUEST = 101;
    // 保存相机拍照的临时图片路径
    private String cameraImagePath;
    private int statusBarHeight;

    /**
     * 扫码结果
     */
    private String cameraResult;

    private Uri photoUri = null;

    private DatabaseManager databaseManager;

    private boolean isPageLoading = false;
    private long lastLoadTime = 0;
    private static final long LOAD_INTERVAL = 500; // 500ms 间隔

    private final Gson gson = new GsonBuilder().create();


    @Override
    @SuppressLint("SetJavaScriptEnabled")
    @RequiresApi(api = Build.VERSION_CODES.O)
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate() called with: savedInstanceState = [" + savedInstanceState + "]");
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        // 1. 设置沉浸式状态栏
        setupImmersiveStatusBar();
        // 初始化页面
        setContentView(R.layout.activity_main);
        requestPermissions(permissionNeedToCheck, 1000);
        // 初始化EventBus
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
        // 延迟启动保活服务，确保UI先初始化
        new Handler().postDelayed(this::startKeepAliveService, 1500);
        // 2. 获取状态栏高度
        statusBarHeight = getStatusBarHeight();
        // 初始化视图
        webView = findViewById(R.id.webView);
        setupWebView();
        // 调试模式下开启 WebView 调试
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        }

        loadLocalWebPage();
        registerJsInterface();

        // 获取 Application 中的数据库管理器
        MyApplication app = (MyApplication) getApplication();
        databaseManager = app.getDatabaseManager();
    }

    private void loadLocalWebPage() {
        try {
            // 根据您的 HTML 结构调整路径
            // String url = "file:///android_asset/xfh5/index.html";
            // webView.loadUrl(url);

            // 直接加载 assets 中的文件
            webView.loadUrl(Util.LOGIN_URL);
            // webView.loadUrl("file:///android_asset/www/index.html");
        } catch (Exception e) {
            Log.e("WebView", "加载页面失败", e);
            Toast.makeText(this, "加载页面失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void setupWebView() {
        WebSettings webSettings = webView.getSettings();

        // 1. 基础设置
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);

        // 2. 文件访问权限（关键配置）
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);

        // 3. 本地文件跨域访问（Android 8.0+ 需要特别注意）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            webSettings.setAllowFileAccessFromFileURLs(true);
            webSettings.setAllowUniversalAccessFromFileURLs(true);
        }

        // 4. 缓存设置
        webSettings.setAppCacheEnabled(true);
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);

        // 设置 AppCache 路径
        String cachePath = getApplicationContext().getCacheDir().getAbsolutePath();
        webSettings.setAppCachePath(cachePath);

        // 5. 视口设置
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NORMAL);

        // 6. 缩放设置
        webSettings.setSupportZoom(false);
        webSettings.setBuiltInZoomControls(false);
        webSettings.setDisplayZoomControls(false);

        // 7. 其他设置
        webSettings.setSaveFormData(true);
        webSettings.setSavePassword(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webSettings.setMediaPlaybackRequiresUserGesture(false);

        // 8. 启用混合内容（如果加载 HTTPS 内容）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }

        // 9. 设置 UserAgent（可选）
        String defaultUserAgent = webSettings.getUserAgentString();
        webSettings.setUserAgentString(defaultUserAgent + " MyApp/1.0");

        // 硬件加速（API 11+）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            webView.setLayerType(WebView.LAYER_TYPE_HARDWARE, null);
        }

        // 关键：不设置任何padding，完全交给H5控制
        webView.setPadding(0, 0, 0, 0);
        webView.setFitsSystemWindows(false);

        // 10. 设置 WebViewClient（关键！）
        webView.setWebViewClient(new CustomWebViewClient());

        // 11. 设置 WebChromeClient
        webView.setWebChromeClient(new CustomWebChromeClient());
    }

    // 自定义 WebViewClient 处理资源加载
    private class CustomWebViewClient extends WebViewClient {

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            return false; // 所有链接都在WebView内打开
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            isPageLoading = true;
            // 预先注入配置
            injectPreConfig();
        }

        private void injectDebugButton() {
            String jsCode =
                    "setTimeout(function() {" +
                            "  console.log('🔧 开始注入Android调试按钮');" +
                            "  " +
                            "  // 移除可能存在的旧按钮" +
                            "  var oldBtn = document.getElementById('android-debug-btn');" +
                            "  if (oldBtn) oldBtn.remove();" +
                            "  " +
                            "  // 创建按钮" +
                            "  var btn = document.createElement('div');" +
                            "  btn.id = 'android-debug-btn';" +
                            "  btn.innerHTML = '🐛';" +
                            "  " +
                            "  // 设置样式 - 确保可见" +
                            "  btn.style.position = 'fixed';" +
                            "  btn.style.bottom = '120px';" +
                            "  btn.style.right = '20px';" +
                            "  btn.style.width = '60px';" +
                            "  btn.style.height = '60px';" +
                            "  btn.style.backgroundColor = 'red';" +
                            "  btn.style.color = 'white';" +
                            "  btn.style.borderRadius = '30px';" +
                            "  btn.style.display = 'flex';" +
                            "  btn.style.alignItems = 'center';" +
                            "  btn.style.justifyContent = 'center';" +
                            "  btn.style.fontSize = '28px';" +
                            "  btn.style.cursor = 'pointer';" +
                            "  btn.style.zIndex = '999999';" +
                            "  btn.style.boxShadow = '0 4px 12px rgba(255,0,0,0.8)';" +
                            "  " +
                            "  // 点击事件" +
                            "  btn.onclick = function() {" +
                            "    console.log('🎯 Android调试按钮被点击');" +
                            "    " +
                            "    // 1. 尝试显示现有的vConsole" +
                            "    if (window.vConsole && window.vConsole.show) {" +
                            "      window.vConsole.show();" +
                            "      return;" +
                            "    }" +
                            "    " +
                            "    // 2. 尝试加载vConsole" +
                            "    if (typeof VConsole !== 'undefined') {" +
                            "      window.vConsole = new VConsole();" +
                            "      window.vConsole.show();" +
                            "    } else {" +
                            "      // 3. 从CDN加载" +
                            "      var script = document.createElement('script');" +
                            "      script.src = 'https://cdn.jsdelivr.net/npm/vconsole@latest/dist/vconsole.min.js';" +
                            "      script.onload = function() {" +
                            "        if (typeof VConsole !== 'undefined') {" +
                            "          window.vConsole = new VConsole();" +
                            "          window.vConsole.show();" +
                            "        }" +
                            "      };" +
                            "      document.head.appendChild(script);" +
                            "    }" +
                            "  };" +
                            "  " +
                            "  // 添加到页面" +
                            "  document.body.appendChild(btn);" +
                            "  console.log('✅ Android调试按钮注入成功');" +
                            "  " +
                            "  // 测试按钮是否真的添加了" +
                            "  console.log('按钮元素:', btn);" +
                            "  console.log('按钮是否在DOM中:', document.body.contains(btn));" +
                            "  console.log('按钮可见性:', btn.offsetParent !== null);" +
                            "}, 1000);";

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                webView.evaluateJavascript(jsCode, null);
            }
        }

        /**
         * 页面加载完成后执行
         */
        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            long currentTime = System.currentTimeMillis();
            // 防抖处理：短时间内只执行一次
            if (currentTime - lastLoadTime < LOAD_INTERVAL) {
                return;
            }
            lastLoadTime = currentTime;
            // 确保是主页面加载完成，不是子框架
            if (!isPageLoading || !url.equals(webView.getUrl())) {
                return;
            }
            isPageLoading = false;
            executePageFinishLogic(url);
            injectDebugButton();
        }

        private void executePageFinishLogic(String url) {
            Log.d("PageLoad", "最终页面加载完成: " + url);

            // 页面完成后再次确认配置
            injectFinalConfig();
        }

        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            // 错误处理时也要保证状态栏正确
            injectPreConfig();
        }

        @SuppressWarnings("deprecation")
        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
            Log.d("WebView", "请求资源: " + url);
            return super.shouldInterceptRequest(view, url);
        }

        @TargetApi(Build.VERSION_CODES.N)
        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            Log.d("WebView", "请求资源: " + request.getUrl().toString());
            return super.shouldInterceptRequest(view, request);
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            super.onReceivedError(view, errorCode, description, failingUrl);
            Log.e("WebView", "加载错误: " + description + " URL: " + failingUrl);

            // 显示错误页面
            String errorHtml = "<html><body><h2>加载失败</h2><p>" + description + "</p></body></html>";
            view.loadDataWithBaseURL(null, errorHtml, "text/html", "UTF-8", null);
        }
    }

    private class CustomWebChromeClient extends WebChromeClient {

        @Override
        public void onPermissionRequest(PermissionRequest request) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                request.grant(request.getResources());
            }
        }

        @Override
        public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
            // 保存回调，后续返回结果给 H5
            MainActivity.this.filePathCallback = filePathCallback;
            showSelectPicDialog();
            return true;
        }

        @Override
        public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
            Log.d("WebViewConsole", consoleMessage.message() + " -- Line: " + consoleMessage.lineNumber() + " of " + consoleMessage.sourceId());
            return true;
        }
    }

    /**
     * 二维码扫码事件
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onQRScannerCallbackEvent(QRScannerEvent event) {
        this.cameraResult = event.getCameraResult();
    }

    private void setupImmersiveStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.TRANSPARENT);

            int systemUiVisibility = window.getDecorView().getSystemUiVisibility();
            systemUiVisibility |= View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
            systemUiVisibility |= View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
            window.getDecorView().setSystemUiVisibility(systemUiVisibility);
        }
    }

    private void injectPreConfig() {
        String jsCode = "window._androidStatusBarHeight = " + statusBarHeight + ";";
        webView.evaluateJavascript(jsCode, null);
    }

    private void injectFinalConfig() {
        String jsCode = String.format("if (typeof window.setupAndroidUI === 'function') {" + "  window.setupAndroidUI({" + "    statusBarHeight: %d," + "    isDarkMode: %b," + "    platform: 'android'" + "  });" + "}", statusBarHeight, !isLightStatusBar());
        webView.evaluateJavascript(jsCode, null);
    }

    // 生成保存照片的临时文件
    //  private File createImageFile() throws IOException {
    //      // 用时间戳作为文件名，确保唯一
    //      String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
    //      String imageFileName = "JPEG_" + timeStamp + "_";
    //      // 保存到应用私有目录下的Pictures文件夹（无需存储权限）
    //      File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
    //      // 创建临时文件
    //      File imageFile = File.createTempFile(imageFileName, ".jpg", storageDir);
    //      // 记录文件路径
    //      cameraImagePath = imageFile.getAbsolutePath();
    //      return imageFile;
    //  }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (storageDir != null && !storageDir.exists()) {
            storageDir.mkdirs();
        }
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }


    /**
     * 选择相机相册弹窗
     */
    private void showSelectPicDialog() {
        SelectPicDialog selectPicDialog = new SelectPicDialog();
        selectPicDialog.setLinkListener(type -> {
            // 相机
            if (type == 0) {
                goCameraFun();
            } else if (type == 1) {
                // 相册
                goAlbum();
            } else {
                Uri[] results = null;
                // 将结果返回给H5
                if (filePathCallback != null) {
                    filePathCallback.onReceiveValue(results);
                    // 重置回调，避免内存泄漏
                    filePathCallback = null;
                }
            }
        });
        selectPicDialog.show(getSupportFragmentManager(), "albumDialog");
    }

    private static final int REQUEST_CAMERA_PERMISSION = 100;

    /**
     * 打开相机（包含权限检查）
     */
    private void goCameraFun() {
        // 检查相机权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            // 请求相机权限
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
            return;
        }
        // 已有权限，直接启动相机
        startCamera();
    }

    /**
     * 启动相机的具体实现
     */
    private void startCamera() {
        try {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

            // 鸿蒙系统兼容性检测
            if (!isCameraAvailable(takePictureIntent)) {
                Toast.makeText(this, "没有找到相机应用", Toast.LENGTH_SHORT).show();
                notifyH5Callback(null);
                return;
            }

            // 创建保存照片的文件
            File photoFile = createImageFile();
            if (Objects.isNull(photoFile)) {
                Toast.makeText(this, "创建文件失败", Toast.LENGTH_SHORT).show();
                notifyH5Callback(null);
                return;
            }

            // 通过FileProvider生成Uri
            photoUri = FileProvider.getUriForFile(MainActivity.this, "com.mydemo.test31.fileprovider", photoFile);

            Log.d(TAG, "相机文件URI: " + photoUri.toString());

            // 授予临时权限并启动相机
            takePictureIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);

            startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "启动相机失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            notifyH5Callback(null);
        }
    }

    /**
     * 兼容鸿蒙系统的相机可用性检测
     */
    private boolean isCameraAvailable(Intent intent) {
        // 如果是鸿蒙系统，直接返回true（鸿蒙系统一定有相机）
        if (isHarmonyOS()) {
            return true;
        }

        // 其他Android系统使用传统检测方法
        return intent.resolveActivity(getPackageManager()) != null;
    }

    /**
     * 鸿蒙官方推荐的检测方法
     */
    private boolean isHarmonyOS() {
        try {
            // 方法1：检测鸿蒙特有的系统属性
            Class<?> systemPropertiesClass = Class.forName("android.os.SystemProperties");
            Method getMethod = systemPropertiesClass.getMethod("get", String.class, String.class);
            // 鸿蒙特有的系统属性
            String harmonyVersion = (String) getMethod.invoke(null, "hw_sc.build.platform.version", "");
            String buildType = (String) getMethod.invoke(null, "ro.build.type", "");
            String productModel = (String) getMethod.invoke(null, "ro.product.model", "");
            // 如果存在鸿蒙平台版本，则是鸿蒙系统
            if (!TextUtils.isEmpty(harmonyVersion)) {
                return true;
            }
            // 鸿蒙系统的构建类型通常是 "user" 或 "userdebug"，但会有特殊标识
            if ("user".equals(buildType) || "userdebug".equals(buildType)) {
                // 进一步检查其他特征
                String buildTags = Build.TAGS;
                if (buildTags != null && buildTags.contains("harmony")) {
                    return true;
                }
            }
        } catch (Exception e) {
            // 忽略异常，继续其他检测方法
            e.printStackTrace();
        }
        return checkHarmonyByReflection();
    }

    /**
     * 通过反射检测鸿蒙特有类
     */
    private boolean checkHarmonyByReflection() {
        String[] harmonyClasses = {"ohos.system.version.SystemVersion",           // 鸿蒙系统版本类
                "ohos.aafwk.ability.Ability",                  // 鸿蒙Ability框架
                "ohos.app.Context",                            // 鸿蒙上下文
                "ohos.global.configuration.Configuration",     // 鸿蒙配置类
                "ohos.bundle.Bundle",                          // 鸿蒙Bundle
                "com.huawei.ohos.global.systemres.SystemRes"   // 鸿蒙系统资源
        };

        for (String className : harmonyClasses) {
            try {
                Class.forName(className);
                return true; // 如果能找到任何一个鸿蒙特有类，就是鸿蒙系统
            } catch (ClassNotFoundException e) {
                // 继续检查下一个类
            }
        }
        return false;
    }

    /**
     * 统一通知H5回调的方法
     */
    private void notifyH5Callback(Uri[] results) {
        if (filePathCallback != null) {
            filePathCallback.onReceiveValue(results);
            filePathCallback = null;
        }
    }

    /**
     * 打开相册
     */
    private void goAlbum() {
        Intent pickIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickIntent.setType("image/* video/*"); // 只选择图片
        startActivityForResult(pickIntent, FILE_CHOOSER_REQUEST_CODE);
    }


    private int getStatusBarHeight() {
        int result = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    private void setLightStatusBar(boolean light) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            View decorView = getWindow().getDecorView();
            int flags = decorView.getSystemUiVisibility();
            if (light) {
                flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            } else {
                flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            }
            decorView.setSystemUiVisibility(flags);
        }
    }

    private boolean isLightStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int flags = getWindow().getDecorView().getSystemUiVisibility();
            return (flags & View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR) != 0;
        }
        return false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    /**
     * 当调用 ActivityCompat.requestPermissions() 或 requestPermissions() 后，
     * 系统会显示权限请求对话框，用户操作后结果会通过这个方法返回。
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean allPermissionGrant = true;
        // 处理相机权限请求
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 权限被授予，重新启动相机
                startCamera();
            } else {
                Toast.makeText(this, "需要相机权限才能拍照", Toast.LENGTH_SHORT).show();
                // 通知H5用户取消了操作
                if (filePathCallback != null) {
                    filePathCallback.onReceiveValue(null);
                    filePathCallback = null;
                }
            }
            // 这里返回，避免与其他权限处理冲突
            return;
        }
        if (requestCode != 1000) {
            return;
        }
        for (int i = 0; i < permissions.length; i++) {
            String permission = permissions[i];
            // 发送通知 (Android 13+)
            if (PERMISSION_POST_NOTIFICATIONS.equals(permission) && Long.parseLong(AndroidVersionUtils.getVersionRelease()) < 13) {
                continue;
            }
            // if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
            //     allPermissionGrant = false;
            //     new AlertDialog.Builder(this)
            //             .setTitle("存在不可用权限")
            //             .setMessage("请在-应用设置-权限-中，允许所有权限")
            //             .setPositiveButton("立即开启", (dialog, which) -> {
            //                 Intent intent = new Intent();
            //                 intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            //                 Uri uri = Uri.fromParts("package", getPackageName(), null);
            //                 intent.setData(uri);
            //                 // 发送特定的请求码
            //                 startActivityForResult(intent, 203);
            //             })
            //             .setNegativeButton("取消", (dialog, which) -> {
            //                 finish();
            //             }).setCancelable(false).show();
            //     break;
            // }
        }
        if (allPermissionGrant) {
            startAndBindService();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // 检查特定的请求码
        if (requestCode == 203) {
            // 重新检查所有需要的权限是否都已授予
            if (!isGrantPermissions(this, Arrays.asList(permissionNeedToCheck))) {
                // 如果还有权限未授予，重新请求权限
                requestPermissions(permissionNeedToCheck, 1000);
            } else {
                // 所有权限都已授予，启动并绑定服务
                startAndBindService();
            }
        }
        Log.i(TAG, "===>相机拍照的图片$cameraImagePath2222");

        // 处理相册选择结果
        if (requestCode == FILE_CHOOSER_REQUEST_CODE) {
            Uri[] results = null;
            if (resultCode == Activity.RESULT_OK && data != null) {
                Uri uri = data.getData();
                ToastUtils.showMessage(MainActivity.this, "相册数据" + data.getData());
                if (uri != null) {
                    results = new Uri[]{uri};
                }
            }

            // 将结果返回给H5
            if (filePathCallback != null) {
                filePathCallback.onReceiveValue(results);
                filePathCallback = null;
            }
        }

        // 处理相机拍照结果 - 修复这里的逻辑
        if (requestCode == REQUEST_TAKE_PHOTO) {
            Uri[] results = null;
            if (resultCode == Activity.RESULT_OK) {
                // 拍照成功，使用 photoUri
                if (photoUri != null) {
                    results = new Uri[]{photoUri};
                    // ToastUtils.showMessage(MainActivity.this, "相机拍照成功: " + photoUri.toString());
                } else {
                    // ToastUtils.showMessage(MainActivity.this, "拍照成功但未获取到图片URI");
                }
            } else {
                // 用户取消了拍照
                // ToastUtils.showMessage(MainActivity.this, "用户取消了拍照");
            }

            // 将结果返回给H5
            if (filePathCallback != null) {
                filePathCallback.onReceiveValue(results);
                filePathCallback = null;
            }

            // 重置 photoUri，避免重复使用
            photoUri = null;
        }

        // 人脸识别图片上传
        if (requestCode == FACE_CAPTURE_REQUEST) {
            if (resultCode == RESULT_OK && data != null) {
                // 获取返回的数据
                String action = data.getStringExtra("action");
                if ("capture".equals(action)) {
                    String base64Image = data.getStringExtra("base64Image");
                    // 文件路径
                    String filePath = data.getStringExtra("filePath");
                    String timestamp = data.getStringExtra("timestamp");
                    int imageWidth = data.getIntExtra("imageWidth", 0);
                    int imageHeight = data.getIntExtra("imageHeight", 0);
                    double quality = data.getDoubleExtra("quality", 0.85);
                    String cameraFacing = data.getStringExtra("cameraFacing");
                    if (filePath != null && !filePath.isEmpty()) {
                        File imageFile = new File(filePath);
                        if (imageFile.exists()) {
                            getTokenFromLocalStorage(token -> uploadWithOkHttp(filePath, token));
                        }
                    }
                }
            } else if (resultCode == RESULT_CANCELED) {
                String error = data != null ? data.getStringExtra("error") : "用户取消";
                Log.d("FaceCapture", "拍照取消: " + error);
            }
        }
    }

    // 添加这个方法
    private void uploadWithOkHttp(String filePath, String token) {
        FileUploader.getInstance().upload(filePath, Util.FACE_IMAGE_URL, token,
                new FileUploader.UploadCallback() {
                    @Override
                    public void onSuccess(String ajaxResult) {
                        runOnUiThread(() -> {
                            // 上传成功，返回结果给WebView
                            // 调用 JS 中的 returnFaceCapture 方法，并传递参数
                            String javascript = String.format(
                                    "javascript:if(window.returnFaceCapture) {" +
                                            "  window.returnFaceCapture('%s', null);" +
                                            "} else {" +
                                            "  alert('回调函数未定义');" +
                                            "}",
                                    ajaxResult.replace("'", "\\'")
                            );
                            webView.evaluateJavascript(javascript, null);
                        });
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            Toast.makeText(MainActivity.this, "上传失败: " + error, Toast.LENGTH_SHORT).show();
                        });
                    }
                });
    }

    // 通过JavaScript获取LocalStorage中的Token
    private void getTokenFromLocalStorage(final ValueCallback<String> callback) {
        try {
            // 从assets读取JS文件
            InputStream is = getAssets().open("js/get_token.js");
            byte[] buffer = new byte[is.available()];
            is.read(buffer);
            is.close();
            String jsCode = new String(buffer, StandardCharsets.UTF_8);
            webView.evaluateJavascript(jsCode, value -> handleTokenResult(value, callback));
        } catch (Exception e) {
            Log.e("LocalStorage", "读取JS文件失败", e);
            if (callback != null) callback.onReceiveValue(null);
        }
    }

    private void handleTokenResult(String value, ValueCallback<String> callback) {
        if (value == null || value.equals("null") || value.isEmpty() || value.equals("\"\"")) {
            Log.d("LocalStorage", "未找到Token");
            if (callback != null) callback.onReceiveValue(null);
            return;
        }

        String token = value.replace("\"", "");
        Log.d("LocalStorage", "获取到Token: " + token);

        if (callback != null) {
            callback.onReceiveValue(token);
        }
    }

    private void startAndBindService() {
        Log.d(TAG, "startAndBindService() called");
        // 设置摄像头角度，仅对camera1有效。
        // PnasConfigUtil.getInstance().setCameraOrientation(90);
        PnasConfigUtil.getInstance().setUseHttps(true);
        // DMS
        PnasConfigUtil.getInstance().setUseDMSConfig(true);
        // 呼叫记录保存在message
        PnasConfigUtil.getInstance().setSaveCallLogInMessage(false);
        // 组呼录音保存在message
        PnasConfigUtil.getInstance().setCallSoundRecordIntoMessage(false);
        // 组呼录音
        PnasConfigUtil.getInstance().setGroupVoiceRecord(true);
        // 日志
        PnasConfigUtil.getInstance().setLogLevel(6);
        // 日志上报
        PnasConfigUtil.getInstance().setLogUploadSwitch(true);
        PnasGisUtil.getInstance().init();
        // 初始化SDK
        PnasSDK.getInstance().init(this);
    }

    /**
     * 协议栈启动结果回调
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onStackStartSuccessCallbackEvent(StackStartSuccessCallbackEvent event) {
        if (event.getIsSuccess() == 1) {
            isInitPnasUserUtilSuccess = true;
            startLogin();
        } else {
            // sip start fail
            isInitPnasUserUtilSuccess = false;
            finish();
        }
    }

    /**
     * 用户注册状态回调
     */
    @Subscribe(threadMode = ThreadMode.MAIN, priority = 255)
    public void onRegistrationStateChangedCallbackEvent(RegistrationStateChangedCallbackEvent event) {
        TrunkingProfileState profileState = event.getProfileState();
    }

    /**
     * 登录
     */
    private void startLogin() {
        if (isInitPnasUserUtilSuccess && isH5Login && StrUtil.isNotBlank(useName)) {
            if (!useName.contains("@")) {
                useName = useName + "@poc.com";
            }
            // Log.i(TAG, "登录的信息 用户名：" + useName + "密码： " + passWord);
            PnasUserUtil.getInstance().login(useName, passWord, pocUrl, null);
            PnasGisUtil.getInstance().login();
        } else if (!PnasUserUtil.getInstance().isLogin()) {
            PnasUserUtil.getInstance().login("50120202@poc.com", "cq123456", "113.204.49.3:8062", null);
            PnasGisUtil.getInstance().login();
        }
        Log.d(TAG, "登录状态： " + PnasUserUtil.getInstance().isLogin());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onStandbyGroupInfoChangedCallbackEvent(StandbyGroupInfoChangedCallbackEvent event) {
        TrunkingConversation conversation;
        TrunkingMessage message;
        PnasErrorCode errorCode;

    }


    /**
     * 启动保活服务
     */
    private void startKeepAliveService() {
        Intent serviceIntent = new Intent(this, KeepAliveService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    private CallReminderDialog callReminderDialog = null;

    /**
     * 呼叫状态变化回调
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onShowCallReminderDialogEvent(ShowCallReminderDialogEvent event) {
        TrunkingCallSession callSession = event.getCallSession();
        if (Objects.isNull(callSession)) {
            return;
        }
        if (!callSession.isAfterEnded()) {
            // 早期媒体/振铃中
            if (callSession.isIncoming() && callSession.isBeforeConfirmed() && callSession.getCallState() == InvState.EARLY) {
                // 弹窗
                acceptDialog(event.callId, callSession);
            } else if (callSession.getCallState() == InvState.CONFIRMED) {
                // 自己接听：callState = 4 接听  对方接听：callState = 5 接听
                OpenVideoActivityEvent openVideoActivityEvent = new OpenVideoActivityEvent(event.callId, event.callSession);
                EventBus.getDefault().post(openVideoActivityEvent);
            }
        } else if (callSession.getCallState() == InvState.DISCONNECTED) {
            if (Objects.nonNull(callReminderDialog)) {
                callReminderDialog.dismiss();
                callReminderDialog = null;
            }
            EventBus.getDefault().post(new CloseVideoActivityEvent(event.callId, event.callSession));
        }
    }

    /**
     * 来电弹窗
     */
    private void acceptDialog(int callId, TrunkingCallSession callSession) {
        if (Objects.nonNull(callReminderDialog)) {
            callReminderDialog.dismiss();
            callReminderDialog = null;
        }
        callReminderDialog = new CallReminderDialog(callSession);
        callReminderDialog.setAnswerBtnListener(new CallReminderDialog.OnBtnClickListener() {
            @Override
            public void ok(CallReminderDialog dialog) {
                dialog.dismiss();
                Intent intent = new Intent(MainActivity.this, MessageUiActivity.class);
                intent.putExtra("comeType", 1);
                intent.putExtra("callSession", callSession);
                startActivity(intent);
            }

            // 点击取消后的逻辑
            @Override
            public void no(CallReminderDialog dialog) {
                dialog.dismiss();
                PnasCallUtil.getInstance().hangupActiveCall();
            }
        });
        callReminderDialog.show(getSupportFragmentManager(), "CallReminderDialog");
    }

    /**
     * 处理返回按钮点击
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // 处理返回按钮点击
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * 话权变化回调
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onFloorStateChangedCallbackEvent(FloorStateChangedCallbackEvent event) {
        Log.d("onFloorState", "" + event.getCallSession().getIsinfo());
    }

    /**
     * 话权变化回调
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onGroupAffiliactionNotifyResultCallbackEvent(GroupAffiliactionNotifyResultCallbackEvent event) {
        Log.d("GroupAffi", "" + event.getGroupNumber() + "," + event.isSuccess());
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        int vId = view.getId();
        Log.d(TAG, "onTouch " + event.getAction());
        return super.onTouchEvent(event);
    }

    @Override
    public void onClick(View v) {

    }


    /**
     * 连接方式
     */
    private void showLinkWayDialog() {
        Toast.makeText(MainActivity.this, "-->调用原生弹窗", Toast.LENGTH_SHORT).show();
        LinkWayDialog dialog = new LinkWayDialog();
        dialog.setLinkListener(item -> {
            Log.i(TAG, "-->选择" + item);
            callType = item;
            dialog.dismiss();
            startUnitDialog();
        });
        dialog.show(getSupportFragmentManager(), "BottomSheetDialog");
    }

    /**
     * 单位弹窗
     */
    public void startUnitDialog() {
        UnitListDialog dialog = new UnitListDialog();
        dialog.setOnOptionSelectedListener(item -> {
            Log.i(TAG, "--->选中 item" + item.getGroupName());
            dialog.dismiss();
            startMemberListDialog(item);
        });
        dialog.show(getSupportFragmentManager(), "fragment");
    }

    /**
     * 成员弹窗列表
     */
    public void startMemberListDialog(TrunkingGroupContact trunkingGroupContact) {
        MemberListDialog dialog = new MemberListDialog(trunkingGroupContact);
        dialog.setOnOptionSelectedListener(item -> {
            Log.i(TAG, "--->选中 item" + item.getName() + "item.getUdn()" + item.getUdn());
            Intent intent = new Intent(MainActivity.this, MessageUiActivity.class);
            intent.putExtra("account", item.getUdn());
            intent.putExtra("callType", callType);
            startActivity(intent);
        });
        dialog.show(getSupportFragmentManager(), "fragment");
    }

    /**
     * 进入到视频界面
     *
     * @param Udn       对方账号名
     * @param mCallType 通话类型 0.语音 1.视频
     */
    private void goMessageUiFun(String Udn, int mCallType) {
        callType = mCallType;
        Intent intent = new Intent(MainActivity.this, MessageUiActivity.class);
        intent.putExtra("account", Udn);
        intent.putExtra("callType", callType);
        intent.putExtra("comeType", 0);
        startActivity(intent);
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    /**
     * 注册 JS 接口，暴露给 H5 调用
     */
    private void registerJsInterface() {
        // 第一个参数：接口实现类，第二个参数：JS 中调用的对象名
        webView.addJavascriptInterface(new NativeInterface(), "AndroidNative");
    }

    /**
     * 原生接口实现类
     * 所有暴露给 JS 的方法必须添加 @JavascriptInterface 注解
     */
    public class NativeInterface {
        /**
         * 弹窗选择
         */
        @JavascriptInterface
        public void showLinkWayFun() {
            // 在主线程中显示 Toast（JS 调用可能在子线程）
            runOnUiThread(MainActivity.this::showLinkWayDialog);
        }

        /**
         * 登录poc
         */
        @JavascriptInterface
        public void loginPoc(String user, String password) {
            // Toast.makeText(MainActivity.this, "用户名：" + user + "密码： " + password, Toast.LENGTH_SHORT).show();
            useName = user;
            passWord = password;
            isH5Login = true;
            startLogin();
            List<User> users = databaseManager.getAllUsers();
            if (CollUtil.isEmpty(users)) {
                if (StrUtil.isNotBlank(user) && StrUtil.isNotBlank(password)) {
                    User dbUser = new User(user, password, user, password);
                    databaseManager.addUser(dbUser);
                }
            } else {
                User dbUser = users.get(0);
                dbUser.setPocUserName(user);
                dbUser.setPocPassword(password);
                databaseManager.updateUser(dbUser);
            }
        }


        /**
         * 登录poc
         */
        @JavascriptInterface
        public void loginPoc(String userName, String password, String pocUserUser, String pocPasswd) {
            useName = userName;
            passWord = password;
            isH5Login = true;
            databaseManager.resetTable();
            User user = new User(userName, password, pocUserUser, pocPasswd);
            databaseManager.addUser(user);
            startLogin();
        }

        /**
         * 重新登录poc
         */
        @JavascriptInterface
        public void relogin() {
            Toast.makeText(MainActivity.this, "重新登录：用户名：" + useName + "密码： " + passWord, Toast.LENGTH_SHORT).show();
            if (!Objects.isNull(useName)) {
                isH5Login = true;
                PnasUserUtil instance = PnasUserUtil.getInstance();
                if (!instance.isLogin()) {
                    instance.relogin();
                }
            }
        }

        /**
         * 进入到视频界面
         *
         * @param user      对方账号名
         * @param mCallType 通话类型 0.语音 1.视频
         */
        @JavascriptInterface
        public void startCallUi(String user, int mCallType) {
            // Toast.makeText(MainActivity.this, "用户名：" + user + "通话类型： " + mCallType, Toast.LENGTH_SHORT).show();
            goMessageUiFun(user, mCallType);
        }

        /**
         * 示例3：原生调用 JS 方法（反向调用）
         */
        @JavascriptInterface
        public void callJsFunction() {
            runOnUiThread(() -> {
                // 调用 JS 中的 showMessage 方法，并传递参数
                webView.evaluateJavascript("javascript:showMessage('来自 Android 原生的调用')", result -> {
                    // JS 方法的返回值（可选处理）
                    Toast.makeText(MainActivity.this, "JS 返回：" + result, Toast.LENGTH_SHORT).show();
                });
            });
        }

        @JavascriptInterface
        public int getStatusBarHeight() {
            return statusBarHeight;
        }

        @JavascriptInterface
        public void setStatusBarStyle(String style) {
            // H5可以动态改变状态栏样式
            runOnUiThread(() -> {
                if ("dark".equals(style)) {
                    setLightStatusBar(false);
                } else {
                    setLightStatusBar(true);
                }
            });
        }

        @JavascriptInterface
        public void setStatusBarColor(String color) {
            runOnUiThread(() -> {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        getWindow().setStatusBarColor(Color.parseColor(color));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }

        /**
         * 打开扫码
         */
        @JavascriptInterface
        public String showQRScanner() {
            // 启动扫描界面
            Intent intent = new Intent(MainActivity.this, CameraQRScannerActivity.class);
            startActivity(intent);
            return cameraResult;
        }

        /**
         * 默认登录
         */
        @JavascriptInterface
        public void defaultLogin() {
            runOnUiThread(() -> {
                // 调用 JS 中的 showMessage 方法，并传递参数
                webView.evaluateJavascript("javascript:showMessage('来自 Android 原生的调用')", result -> {
                    // JS 方法的返回值（可选处理）
                    Toast.makeText(MainActivity.this, "JS 返回：" + result, Toast.LENGTH_SHORT).show();
                });
            });
        }

        @JavascriptInterface
        public void startFaceCapture() {
            runOnUiThread(() -> {
                try {
                    Intent intent = new Intent(MainActivity.this, FaceCaptureActivity.class);
                    startActivityForResult(intent, FACE_CAPTURE_REQUEST);
                } catch (Exception e) {
                    sendErrorToWeb("启动人脸采集失败: " + e.getMessage());
                }
            });
        }

        @JavascriptInterface
        public String getDeviceInfo() {
            Map<String, Object> info = new HashMap<>();
            info.put("manufacturer", Build.MANUFACTURER);
            info.put("model", Build.MODEL);
            info.put("androidVersion", Build.VERSION.RELEASE);
            info.put("sdkVersion", Build.VERSION.SDK_INT);

            android.view.Display display = getWindowManager().getDefaultDisplay();
            android.graphics.Point size = new android.graphics.Point();
            display.getSize(size);
            info.put("screenWidth", size.x);
            info.put("screenHeight", size.y);
            info.put("density", getResources().getDisplayMetrics().density);
            return gson.toJson(info);
        }

        @JavascriptInterface
        public void faceResult(String result) {
            Log.d("AndroidNative", "收到JS回调: " + result);
            runOnUiThread(() -> {
                System.out.println(result);
            });
        }
    }

    private void sendErrorToWeb(final String error) {
        final String jsCode = "if (window.onFaceCaptureError) { window.onFaceCaptureError('" + error.replace("'", "\\'") + "'); }";
        new Handler(Looper.getMainLooper()).post(() -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                webView.evaluateJavascript(jsCode, null);
            } else {
                webView.loadUrl("javascript:" + jsCode);
            }
        });
    }

}