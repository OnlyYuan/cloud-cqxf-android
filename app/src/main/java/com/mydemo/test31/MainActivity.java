package com.mydemo.test31;

import static com.mydemo.test31.util.Util.h5Url;
import static com.mydemo.test31.util.Util.pocUrl;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.mptt.media.modules.uvc.utils.ToastUtils;
import com.mpttpnas.api.TrunkingCallSession;
import com.mpttpnas.api.TrunkingConversation;
import com.mpttpnas.api.TrunkingGroupContact;
import com.mpttpnas.api.TrunkingLocalContact;
import com.mpttpnas.api.TrunkingMessage;
import com.mpttpnas.pnas.agent.PnasErrorCode;
import com.mpttpnas.pnaslibraryapi.PnasCallUtil;
import com.mpttpnas.pnaslibraryapi.PnasConfigUtil;
import com.mpttpnas.pnaslibraryapi.PnasContactUtil;
import com.mpttpnas.pnaslibraryapi.PnasSDK;
import com.mpttpnas.pnaslibraryapi.PnasUserUtil;
import com.mpttpnas.pnaslibraryapi.callback.CallStateChangedCallbackEvent;
import com.mpttpnas.pnaslibraryapi.callback.FloorStateChangedCallbackEvent;
import com.mpttpnas.pnaslibraryapi.callback.GroupAffiliactionNotifyResultCallbackEvent;
import com.mpttpnas.pnaslibraryapi.callback.StackStartSuccessCallbackEvent;
import com.mpttpnas.pnaslibraryapi.callback.StandbyGroupInfoChangedCallbackEvent;
import com.mydemo.test31.dialog.CallReminderDialog;
import com.mydemo.test31.dialog.LinkWayDialog;
import com.mydemo.test31.dialog.MemberListDialog;
import com.mydemo.test31.dialog.SelectPicDialog;
import com.mydemo.test31.dialog.UnitListDialog;
import com.mydemo.test31.event.CloseVideoActivityEvent;
import com.mydemo.test31.event.OpenVideoActivityEvent;
import com.mydemo.test31.service.KeepAliveService;
import com.mydemo.test31.util.AndroidVersionUtils;
import com.mydemo.test31.util.MyProvider;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jivesoftware.smack.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

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

    public final static String[] permissionNeedToCheck = {
            PERMISSION_READ_PHONE_STATE,           // 读取手机状态
            PERMISSION_ACCESS_FINE_LOCATION,       // 精确位置
            PERMISSION_ACCESS_COARSE_LOCATION,     // 粗略位置
            PERMISSION_CAMERA,                     // 相机
            PERMISSION_RECORD_AUDIO,               // 录音
            PERMISSION_READ_MEDIA_IMAGES,
            PERMISSION_READ_EXTERNAL_STORAGE,      // 读取外部存储
            PERMISSION_WRITE_EXTERNAL_STORAGE,     // 写入外部存储
            PERMISSION_POST_NOTIFICATIONS,         // 发送通知 (Android 13+)
            PERMISSION_AUDIO_SETTINGS              // 音频设置
    };

    public static boolean isGrantPermissions(Activity activity, List<String> permissionList) {
        if (activity == null || permissionList.isEmpty()) {
            return false;
        }
        for (int i = 0; i < permissionList.size(); i++) {
            if (activity.checkSelfPermission(
                    permissionList.get(i)) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    TextView goDialogBtn;

    WebView webView;

    private SurfaceView renderView;

    private int callType = 0;//建立连接方式 0.语音  1.视频

    //判断poc是否初始化完成
    private boolean isInitPnasUserUtilSuccess = false;
    //h5是否调用了登录
    private boolean isH5Login = false;

    // h5传入的用户名
    private String useName = "";

    // h5传入的密码
    private String passWord = "";

    private ValueCallback<Uri[]> filePathCallback; // 用于向H5返回选择结果
    private static final int FILE_CHOOSER_REQUEST_CODE = 1001;
    private static final int REQUEST_TAKE_PHOTO = 1002;
    private String cameraImagePath; // 保存相机拍照的临时图片路径

    @Override
    @RequiresApi(api = Build.VERSION_CODES.O)
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate() called with: savedInstanceState = [" + savedInstanceState + "]");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        requestPermissions(permissionNeedToCheck, 1000);
        EventBus.getDefault().register(this);

        renderView = new SurfaceView(this);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        lp.gravity = Gravity.CENTER;
        renderView.setLayoutParams(lp);
        renderView.getHolder().addCallback(new VideoSurfaceCallback(PnasCallUtil.VideoCallWindow.LAUNCH_REMOTE_VIDEO));
        renderView.setVisibility(View.VISIBLE);
        startKeepAliveService();
        initView();
    }

    /**
     * 初始化视图
     */
    private void initView() {
        webView = findViewById(R.id.webView);
        initWebViewSettings();
        webView.loadUrl(h5Url);
    }

    /**
     * 初始化 WebView 设置
     */
    private void initWebViewSettings() {
        WebSettings webSettings = webView.getSettings();
        // 启用 JavaScript
        webSettings.setJavaScriptEnabled(true);
        // 允许 JS 弹窗
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        // 支持缩放
        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(true);
        //设置cookie缓存
        webSettings.setDomStorageEnabled(true);
        webSettings.setAppCacheEnabled(true);
        webSettings.setSaveFormData(true);
        webSettings.setSavePassword(true);
        webSettings.setAllowUniversalAccessFromFileURLs(true);

        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);

        // 3. 允许自动播放媒体（可选，避免H5调用摄像头后需要手动触发）
        webSettings.setMediaPlaybackRequiresUserGesture(false);

        // 允许混合内容（http 和 https 混合加载）
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }
        // 设置 WebChromeClient 以支持 JS 弹窗
        webView.setWebChromeClient(new WebChromeClient(){
            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                // 保存回调，后续返回结果给 H5
                MainActivity.this.filePathCallback = filePathCallback;
                showSelectPicDialog();
                return true;
            }
        });
        registerJsInterface();
    }


    // 生成保存照片的临时文件
    private File createImageFile() throws IOException {
        // 用时间戳作为文件名，确保唯一
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        // 保存到应用私有目录下的Pictures文件夹（无需存储权限）
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        // 创建临时文件
        File imageFile = File.createTempFile(imageFileName, ".jpg", storageDir);
        // 记录文件路径
        cameraImagePath = imageFile.getAbsolutePath();
        return imageFile;
    }


    /**
     * 选择相机相册弹窗
     */
    private void showSelectPicDialog(){
        SelectPicDialog selectPicDialog = new SelectPicDialog();
        selectPicDialog.setLinkListener(new SelectPicDialog.OnSelectPicDialogListener() {
            @Override
            public void onOptionSelected(int type) {
                if (type ==0){//相机
                    goCameraFun();
                }else {//相册
                    goAlbum();
                }
            }
        });
        selectPicDialog.show(getSupportFragmentManager(),"albumDialog");
    }

    /**
     * 打开相机
     */
    private void goCameraFun(){
        // 创建启动相机的Intent
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        // 检查是否有相机应用可处理
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // 创建保存照片的文件
            File photoFile = null;
            try {
                photoFile = createImageFile(); // 生成临时文件
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }

            if (photoFile != null) {
                // 通过FileProvider生成content://格式的Uri（避免FileUriExposedException）
                Uri photoUri = MyProvider.getUriForFile(
                        MainActivity.this,
                        "com.mydemo.test31.fileprovider", // 与Manifest中一致
                        photoFile
                );

                // 告诉相机将照片保存到该Uri
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                // 启动相机，等待结果返回
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }

    /**
     * 打开相册
     */
    private void goAlbum(){
        Intent pickIntent = new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickIntent.setType("image/* video/*"); // 只选择图片
        startActivityForResult(pickIntent, FILE_CHOOSER_REQUEST_CODE);
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
        }

        /**
         * 重新登录poc
         */
        @JavascriptInterface
        public void relogin() {
            Toast.makeText(MainActivity.this, "重新登录：用户名："
                    + useName + "密码： " + passWord, Toast.LENGTH_SHORT).show();
            if (!StringUtils.isNullOrEmpty(useName)) {
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
            Toast.makeText(MainActivity.this, "用户名：" + user + "通话类型： " + mCallType, Toast.LENGTH_SHORT).show();
            goMessageUiFun(user, mCallType);
        }

        /**
         * 示例3：原生调用 JS 方法（反向调用）
         */
        @JavascriptInterface
        public void callJsFunction() {
            runOnUiThread(() -> {
                // 调用 JS 中的 showMessage 方法，并传递参数
                webView.evaluateJavascript("javascript:showMessage('来自 Android 原生的调用')",
                        result -> {
                            // JS 方法的返回值（可选处理）
                            Toast.makeText(MainActivity.this, "JS 返回：" + result, Toast.LENGTH_SHORT).show();
                        });
            });
        }
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
        if (requestCode != 1000) {
            return;
        }
        for (int i = 0; i < permissions.length; i++) {
            String permission = permissions[i];
            // 发送通知 (Android 13+)
            if (PERMISSION_POST_NOTIFICATIONS.equals(permission) &&
                    Long.parseLong(AndroidVersionUtils.getVersionRelease()) < 13) {
                continue;
            }
            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                allPermissionGrant = false;
                new AlertDialog.Builder(this)
                        .setTitle("存在不可用权限")
                        .setMessage("请在-应用设置-权限-中，允许所有权限")
                        .setPositiveButton("立即开启", (dialog, which) -> {
                            Intent intent = new Intent();
                            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            Uri uri = Uri.fromParts("package", getPackageName(), null);
                            intent.setData(uri);
                            // 发送特定的请求码
                            startActivityForResult(intent, 203);
                        })
                        .setNegativeButton("取消", (dialog, which) -> {
                            finish();
                        }).setCancelable(false).show();
                break;
            }
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
        Log.i(TAG,"===>相机拍照的图片$cameraImagePath2222");

        //相册
        if (requestCode == FILE_CHOOSER_REQUEST_CODE) {
            Uri[] results = null;

            // 处理返回结果（用户选择了图片或拍照成功）
            if (resultCode == Activity.RESULT_OK) {
                if (data != null) {
                    // 相册选择的图片（有数据返回）
                    Uri uri = data.getData();
                    if (uri != null) {
                        results = new Uri[]{uri};
                    }
                }
            }

            // 将结果返回给H5
            if (filePathCallback != null) {
                filePathCallback.onReceiveValue(results);
                filePathCallback = null; // 重置回调，避免内存泄漏
            }
        }

        //相机
        if (requestCode ==  REQUEST_TAKE_PHOTO &&resultCode == Activity.RESULT_OK ){
                // 相机拍照的图片（无data返回，用临时路径）
            ToastUtils.showMessage(this,"1111相机拍照的图片"+cameraImagePath);
            Uri[] results = null;
            if (cameraImagePath!=null){
                results = new Uri[]{Uri.parse(cameraImagePath)}; // cameraImagePath 为拍照临时路径
            }

            // 将结果返回给H5
            if (filePathCallback != null) {
                filePathCallback.onReceiveValue(results);
                filePathCallback = null; // 重置回调，避免内存泄漏
            }
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
        PnasConfigUtil.getInstance().setSaveCallLogInMessage(true);
        //  组呼录音保存在message
        PnasConfigUtil.getInstance().setCallSoundRecordIntoMessage(true);
        // 日志
        PnasConfigUtil.getInstance().setLogLevel(6);
        // 初始化SDK
        PnasSDK.getInstance().init(this);
    }

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
     * 登录
     */
    private void startLogin() {
        if (isInitPnasUserUtilSuccess && isH5Login) {
            if (!useName.contains("@")) {
                useName = useName + "@poc.com";
            }
            // Log.i(TAG, "登录的信息 用户名：" + useName + "密码： " + passWord);
            PnasUserUtil.getInstance().login(useName, passWord, pocUrl, null);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onStandbyGroupInfoChangedCallbackEvent(StandbyGroupInfoChangedCallbackEvent event) {
        TrunkingConversation conversation;
        TrunkingMessage message;
        PnasErrorCode errorCode;
        //  PnasContactUtil.getInstance().getAllUserContactList()
    }


    /**
     * 启动保活服务
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void startKeepAliveService() {
        Intent intent = new Intent(this, KeepAliveService.class);
        startForegroundService(intent);
    }

    TrunkingCallSession callSession;

    /**
     * 呼叫状态变化回调
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onCallStateChangedCallbackEvent(CallStateChangedCallbackEvent event) {
        callSession = event.getCallSession();
        if (callSession != null && !callSession.isAfterEnded()) {
            if (callSession.isIncoming() && callSession.isBeforeConfirmed()) {
                // 弹窗
                acceptDialog(event);
            } else if (callSession.getCallState() == 4
                    || callSession.getCallState() == 5) {
                // 自己接听：callState = 4 接听  对方接听：callState = 5 接听
                if (callSession.isVideoCall()) {
                    OpenVideoActivityEvent openVideoActivityEvent = new OpenVideoActivityEvent(event.callId,
                            event.callSession);
                    openVideoActivityEvent.setCallReminderDialog(callReminderDialog);
                    EventBus.getDefault().post(openVideoActivityEvent);
                }
            }
        } else {
            if (Objects.nonNull(callReminderDialog)) {
                callReminderDialog.dismiss();
                callReminderDialog = null;
            }
            EventBus.getDefault().post(new CloseVideoActivityEvent(event.callId, event.callSession));
        }
    }

    // 处理返回按钮点击
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

    public void getContantList() {
        List<TrunkingGroupContact> list = PnasContactUtil.getInstance().getMyGroupList();
        for (int i = 0; i < list.size(); i++) {
            Log.d(TAG, "列表数据：" + list.get(i).toString());
        }
        List<TrunkingLocalContact> memberList = PnasContactUtil.getInstance().getGroupContactList(list.get(0).getGroupGdn());
        for (int i = 0; i < memberList.size(); i++) {
            Log.d(TAG, "成员数据：" + memberList.get(i).getName());
        }
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

    private CallReminderDialog callReminderDialog = null;

    /**
     * 来电弹窗
     */
    private void acceptDialog(CallStateChangedCallbackEvent event) {
        if (Objects.nonNull(callReminderDialog)) {
            return;
        }
        callReminderDialog = new CallReminderDialog(callSession);
        callReminderDialog.show(getSupportFragmentManager(), "CallReminderDialog");
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
        startActivity(intent);
    }

}