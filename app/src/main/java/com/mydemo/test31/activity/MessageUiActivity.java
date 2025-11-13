package com.mydemo.test31.activity;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.mpttpnas.api.TrunkingCallSession;
import com.mpttpnas.pnaslibraryapi.PnasCallUtil;
import com.mpttpnas.pnaslibraryapi.PnasContactUtil;
import com.mpttpnas.pnaslibraryapi.callback.FloorStateChangedCallbackEvent;
import com.mpttpnas.pnaslibraryapi.callback.GroupAffiliactionNotifyResultCallbackEvent;
import com.mpttpnas.pnaslibraryapi.callback.StandbyGroupInfoChangedCallbackEvent;
import com.mydemo.test31.R;
import com.mydemo.test31.callback.VideoSurfaceCallback;
import com.mydemo.test31.event.CloseVideoActivityEvent;
import com.mydemo.test31.event.OpenVideoActivityEvent;
import com.mydemo.test31.util.MicMuteManager;
import com.mydemo.test31.util.MuteManager;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Objects;

public class MessageUiActivity extends AppCompatActivity {

    private final String TAG = "MessageUiActivity";
    //连接对象的账户名
    private String accountString = "";
    //断开
    private Button cancelBtn = null;
    //麦克风
    private ImageView micBtn = null;
    //音量
    private ImageView voiceBtn = null;
    private ImageView closeBtn = null;
    private ImageView volumeBtn = null;
    private ImageView cameraSwitchBtn = null;

    // 视频通话区域
    private FrameLayout loVideo = null;
    private SurfaceView remoteRenderView;  // 远程视频画面
    private SurfaceView localRenderView;   // 本地视频画面
    private ConstraintLayout unLinkBg;

    // 小窗口相关
    private RelativeLayout smallVideoContainer;
    private FrameLayout smallVideoView;

    // 创建连接的方式 0.语音 1.视频
    private int callType = 0;
    // 进入方式方式，0.根据列表进入 1.接收电话进入
    private int comeType = 0;

    // 声音开关状态  true是开  false是关
    private boolean isVoiceOn = true;

    // mic的状态  true是禁音，false未禁音
    private boolean isMicMuted = false;

    // mic禁音
    MicMuteManager micManager = null;
    // 声音禁音
    MuteManager voiceMuteManager = null;

    // 用于保存原始音量，以便恢复
    private int originalVolume = 0;

    // 通话状态跟踪
    private boolean isCallActive = false;
    private boolean isCallEnded = false;

    // 小窗口拖动相关变量
    private float dX, dY;

    // 窗口切换状态：true-小窗口显示本地视频，大窗口显示远程视频；false-小窗口显示远程视频，大窗口显示本地视频
    private boolean isLocalVideoInSmallWindow = true;

    // 摄像头状态：true-前置摄像头，false-后置摄像头
    private boolean isFrontCamera = true;

    // 双击检测相关
    private Handler doubleClickHandler = new Handler();
    private int clickCount = 0;
    private static final int DOUBLE_CLICK_TIME = 300; // 双击时间间隔（毫秒）
    private Runnable doubleClickRunnable;

    // 防止快速点击导致的重复切换
    private boolean isSwitching = false;
    private static final long SWITCH_COOLDOWN = 800; // 切换冷却时间（毫秒），适当延长

    // 频繁操作检测
    private long lastSwitchTime = 0;
    private static final long FREQUENT_OPERATION_LIMIT = 2000; // 2秒内操作限制
    private int switchCountInShortTime = 0;
    private boolean isFrequentOperationBlocked = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message_ui);
        initView();
        initData();
        initListener();
    }

    private void initData() {
        accountString = getIntent().getStringExtra("account");
        callType = getIntent().getIntExtra("callType", 0);
        comeType = getIntent().getIntExtra("comeType", 0);
        EventBus.getDefault().register(this);

        // 在 Activity 中使用
        micManager = new MicMuteManager(this);
        isMicMuted = micManager.isMicMuted();

        if (comeType == 1) {
            // 接收来电
            TrunkingCallSession callSession = getIntent().getParcelableExtra("callSession");
            if (Objects.nonNull(callSession)) {
                // 检查通话状态，避免接听已结束的通话
                if (callSession.isAfterEnded()) {
                    Log.w(TAG, "来电已超时或已取消，无法接听");
                    Toast.makeText(this, "来电已结束", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }

                accountString = callSession.getRemoteContact();
                Log.i(TAG, "接听来电 --> 对方:" + accountString + " 通话状态:" + callSession.getCallState());

                // 接听通话 0是成功，其他为失败
                int acceptResult = PnasCallUtil.getInstance().acceptCall(callSession.getCallId());
                if (acceptResult == 0) {
                    Log.i(TAG, "接听成功");
                    isCallActive = true;
                    unLinkBg.setVisibility(View.GONE);
                    loVideo.setVisibility(View.VISIBLE);
                    setupVideoViews(); // 设置视频视图
                } else {
                    Log.e(TAG, "接听失败");
                    Toast.makeText(this, "接听失败", Toast.LENGTH_SHORT).show();
                    finish();
                }
            } else {
                Log.e(TAG, "通话会话为空");
                Toast.makeText(this, "通话信息错误", Toast.LENGTH_SHORT).show();
                finish();
            }
        } else if (comeType == 0) {
            // 主动拨号
            Log.i(TAG, "主动拨号 --> 对方:" + accountString + " 通话类型:" + callType);
            createLink();
            isCallActive = true;
            unLinkBg.setVisibility(View.GONE);
            loVideo.setVisibility(View.VISIBLE);
            setupVideoViews(); // 设置视频视图
        }
    }

    private void initView() {
        cancelBtn = findViewById(R.id.cancelBtn);
        loVideo = findViewById(R.id.loVideo);
        micBtn = findViewById(R.id.mic_btn);
        voiceBtn = findViewById(R.id.voice_Btn);
        closeBtn = findViewById(R.id.close_btn);
        unLinkBg = findViewById(R.id.unLinkBg);
        volumeBtn = findViewById(R.id.volume_btn);
        cameraSwitchBtn = findViewById(R.id.cameraSwitchBtn);

        // 小窗口相关视图
        smallVideoContainer = findViewById(R.id.smallVideoContainer);
        smallVideoView = findViewById(R.id.smallVideoView);

        initVoiceMute();
        setMicImage();
        setVoiceImage();

        // 初始化双击检测
        initDoubleClickDetection();
    }

    /**
     * 初始化双击检测
     */
    private void initDoubleClickDetection() {
        doubleClickRunnable = new Runnable() {
            @Override
            public void run() {
                clickCount = 0;
            }
        };
    }

    /**
     * 检查是否操作过于频繁
     */
    private boolean checkFrequentOperation() {
        long currentTime = System.currentTimeMillis();

        // 如果已经被限制操作，检查限制是否解除
        if (isFrequentOperationBlocked) {
            if (currentTime - lastSwitchTime > FREQUENT_OPERATION_LIMIT) {
                // 解除限制
                isFrequentOperationBlocked = false;
                switchCountInShortTime = 0;
                Log.i(TAG, "频繁操作限制已解除");
            } else {
                // 仍在限制期内
                long remainingTime = FREQUENT_OPERATION_LIMIT - (currentTime - lastSwitchTime);
                Toast.makeText(this, "操作过于频繁，请稍后再试 (" + (remainingTime / 1000) + "秒)",
                        Toast.LENGTH_SHORT).show();
                return true;
            }
        }

        // 检查短时间内操作次数
        if (currentTime - lastSwitchTime < FREQUENT_OPERATION_LIMIT) {
            switchCountInShortTime++;
            Log.i(TAG, "短时间内切换次数: " + switchCountInShortTime);

            // 如果2秒内操作超过3次，触发限制
            if (switchCountInShortTime >= 3) {
                isFrequentOperationBlocked = true;
                Toast.makeText(this, "操作过于频繁，请等待" + (FREQUENT_OPERATION_LIMIT / 1000) + "秒后再试",
                        Toast.LENGTH_SHORT).show();
                return true;
            }
        } else {
            // 重置计数
            switchCountInShortTime = 1;
        }

        lastSwitchTime = currentTime;
        return false;
    }

    /**
     * 设置视频视图
     */
    private void setupVideoViews() {
        // 清除之前的视图
        loVideo.removeAllViews();
        smallVideoView.removeAllViews();

        // 创建远程视频画面
        remoteRenderView = new SurfaceView(this);
        FrameLayout.LayoutParams remoteLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        remoteLp.gravity = Gravity.CENTER;
        remoteRenderView.setLayoutParams(remoteLp);

        // 创建本地视频画面
        localRenderView = new SurfaceView(this);
        FrameLayout.LayoutParams localLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        localRenderView.setLayoutParams(localLp);

        // 初始状态：大窗口显示远程视频，小窗口显示本地视频
        setupInitialVideoLayout();
    }

    /**
     * 设置初始视频布局
     */
    private void setupInitialVideoLayout() {
        if (isLocalVideoInSmallWindow) {
            // 大窗口显示远程视频
            setupRemoteVideoInLargeWindow();
            // 小窗口显示本地视频
            setupLocalVideoInSmallWindow();
            // 显示摄像头切换按钮
            cameraSwitchBtn.setVisibility(View.VISIBLE);
        } else {
            // 大窗口显示本地视频
            setupLocalVideoInLargeWindow();
            // 小窗口显示远程视频
            setupRemoteVideoInSmallWindow();
            // 隐藏摄像头切换按钮（远程视频不需要切换摄像头）
            cameraSwitchBtn.setVisibility(View.GONE);
        }
    }

    /**
     * 在大窗口显示远程视频
     */
    private void setupRemoteVideoInLargeWindow() {
        if (remoteRenderView != null) {
            remoteRenderView.getHolder().removeCallback(null);
            loVideo.addView(remoteRenderView, 0);
            remoteRenderView.getHolder().addCallback(new VideoSurfaceCallback(PnasCallUtil.VideoCallWindow.LAUNCH_REMOTE_VIDEO));
            remoteRenderView.setVisibility(View.VISIBLE);
        }
    }

    /**
     * 在小窗口显示远程视频
     */
    private void setupRemoteVideoInSmallWindow() {
        if (remoteRenderView != null) {
            remoteRenderView.getHolder().removeCallback(null);
            smallVideoView.addView(remoteRenderView);
            remoteRenderView.getHolder().addCallback(new VideoSurfaceCallback(PnasCallUtil.VideoCallWindow.LAUNCH_REMOTE_VIDEO));
            remoteRenderView.setVisibility(View.VISIBLE);
        }
    }

    /**
     * 在大窗口显示本地视频
     */
    private void setupLocalVideoInLargeWindow() {
        if (localRenderView != null) {
            localRenderView.getHolder().removeCallback(null);
            loVideo.addView(localRenderView, 0);
            localRenderView.getHolder().addCallback(new VideoSurfaceCallback(PnasCallUtil.VideoCallWindow.LAUNCH_LOCAL_VIDEO));
            localRenderView.setVisibility(View.VISIBLE);
        }
    }

    /**
     * 在小窗口显示本地视频
     */
    private void setupLocalVideoInSmallWindow() {
        if (localRenderView != null) {
            localRenderView.getHolder().removeCallback(null);
            smallVideoView.addView(localRenderView);
            localRenderView.getHolder().addCallback(new VideoSurfaceCallback(PnasCallUtil.VideoCallWindow.LAUNCH_LOCAL_VIDEO));
            localRenderView.setVisibility(View.VISIBLE);
        }
    }

    /**
     * 切换视频窗口
     */
    private void switchVideoWindows() {
        // 防止快速重复点击
        if (isSwitching) {
            Log.i(TAG, "正在切换中，请稍后");
            Toast.makeText(this, "正在切换中，请稍后", Toast.LENGTH_SHORT).show();
            return;
        }

        // 检查频繁操作限制
        if (checkFrequentOperation()) {
            return;
        }

        if (!isCallActive || isCallEnded) {
            Log.w(TAG, "通话未激活或已结束，无法切换视频");
            return;
        }

        isSwitching = true;
        isLocalVideoInSmallWindow = !isLocalVideoInSmallWindow;

        Log.i(TAG, "切换视频窗口，当前模式：" + (isLocalVideoInSmallWindow ? "小窗口显示本地视频" : "小窗口显示远程视频"));

        try {
            // 清除之前的视图
            loVideo.removeAllViews();
            smallVideoView.removeAllViews();

            if (isLocalVideoInSmallWindow) {
                // 切换到大窗口显示远程视频，小窗口显示本地视频
                setupRemoteVideoInLargeWindow();
                setupLocalVideoInSmallWindow();
                cameraSwitchBtn.setVisibility(View.VISIBLE);
                Toast.makeText(this, "已切换到对方画面", Toast.LENGTH_SHORT).show();
            } else {
                // 切换到大窗口显示本地视频，小窗口显示远程视频
                setupLocalVideoInLargeWindow();
                setupRemoteVideoInSmallWindow();
                cameraSwitchBtn.setVisibility(View.GONE);
                Toast.makeText(this, "已切换到本人画面", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "切换视频窗口失败", e);
            Toast.makeText(this, "切换失败，请重试", Toast.LENGTH_SHORT).show();
        } finally {
            // 重置切换状态
            doubleClickHandler.postDelayed(() -> {
                isSwitching = false;
            }, SWITCH_COOLDOWN);
        }
    }

    /**
     * 双击小窗口放大
     */
    private void handleDoubleClick() {
        if (isSwitching) {
            return;
        }

        // 检查频繁操作限制
        if (checkFrequentOperation()) {
            return;
        }

        Log.i(TAG, "双击小窗口，切换大小窗口");
        switchVideoWindows();
    }

    /**
     * 切换摄像头
     */
    private void switchCamera() {
        if (!isCallActive || isCallEnded) {
            Log.w(TAG, "通话未激活或已结束，无法切换摄像头");
            return;
        }

        // 检查频繁操作限制
        if (checkFrequentOperation()) {
            return;
        }

        isFrontCamera = !isFrontCamera;
        try {
            // 调用SDK的摄像头切换方法
            PnasCallUtil.getInstance().switchCamera();
            Log.i(TAG, "摄像头切换成功，当前摄像头：" + (isFrontCamera ? "前置" : "后置"));
            Toast.makeText(this, isFrontCamera ? "前置摄像头" : "后置摄像头", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "摄像头切换失败", e);
            Toast.makeText(this, "摄像头切换失败", Toast.LENGTH_SHORT).show();
            // 发生异常时恢复状态
            isFrontCamera = !isFrontCamera;
        }
    }

    private void initVoiceMute() {
        // 保存原始音量
        voiceMuteManager = new MuteManager(this);
        originalVolume = voiceMuteManager.getCurrentMusicVolume();
    }

    private void initListener() {
        cancelBtn.setOnClickListener(view -> {
            // 挂断
            if (isCallActive && !isCallEnded) {
                Log.i(TAG, "用户主动挂断通话");
                PnasCallUtil.getInstance().hangupActiveCall();
                setCallEnded();
            }
            finish();
        });

        // 禁音
        voiceBtn.setOnClickListener(view -> {
            setVoiceFun();
        });

        // 麦克风
        micBtn.setOnClickListener(view -> setMicFun());

        // 切换视频窗口按钮 - 添加防抖处理
        volumeBtn.setOnClickListener(view -> {
            if (!isSwitching) {
                switchVideoWindows();
            } else {
                Toast.makeText(this, "正在切换中，请稍后", Toast.LENGTH_SHORT).show();
            }
        });

        // 摄像头切换按钮
        cameraSwitchBtn.setOnClickListener(view -> switchCamera());

        closeBtn.setOnClickListener(view -> {
            if (isCallActive && !isCallEnded) {
                Log.i(TAG, "用户主动挂断通话");
                PnasCallUtil.getInstance().hangupActiveCall();
                setCallEnded();
            }
            finish();
        });

        // 小窗口拖动监听和双击监听
        setupSmallVideoTouchListener();
    }

    /**
     * 设置小窗口触摸监听（拖动 + 双击）
     */
    private void setupSmallVideoTouchListener() {
        smallVideoContainer.setOnTouchListener(new View.OnTouchListener() {
            private long lastClickTime = 0;

            @Override
            public boolean onTouch(View view, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        dX = view.getX() - event.getRawX();
                        dY = view.getY() - event.getRawY();

                        // 双击检测
                        long clickTime = System.currentTimeMillis();
                        if (clickTime - lastClickTime < DOUBLE_CLICK_TIME) {
                            // 双击事件
                            handleDoubleClick();
                            lastClickTime = 0;
                            return true; // 消耗双击事件，不触发拖动
                        } else {
                            lastClickTime = clickTime;
                        }
                        break;

                    case MotionEvent.ACTION_MOVE:
                        // 如果是双击后的移动，不处理拖动
                        if (System.currentTimeMillis() - lastClickTime < DOUBLE_CLICK_TIME) {
                            return true;
                        }

                        float newX = event.getRawX() + dX;
                        float newY = event.getRawY() + dY;

                        // 限制小窗口在屏幕范围内
                        FrameLayout parent = (FrameLayout) view.getParent();
                        if (parent != null) {
                            int maxX = parent.getWidth() - view.getWidth();
                            int maxY = parent.getHeight() - view.getHeight();

                            newX = Math.max(0, Math.min(newX, maxX));
                            newY = Math.max(0, Math.min(newY, maxY));
                        }

                        view.animate()
                                .x(newX)
                                .y(newY)
                                .setDuration(0)
                                .start();
                        break;

                    case MotionEvent.ACTION_UP:
                        // 如果不是双击，处理拖动结束的吸附
                        if (System.currentTimeMillis() - lastClickTime >= DOUBLE_CLICK_TIME) {
                            snapToEdge(view);
                        }
                        break;
                }
                return true;
            }
        });
    }

    /**
     * 小窗口吸附到边缘
     */
    private void snapToEdge(View view) {
        float x = view.getX();
        float y = view.getY();
        FrameLayout parent = (FrameLayout) view.getParent();

        if (parent != null) {
            int centerX = parent.getWidth() / 2;

            // 如果小窗口在屏幕左侧，吸附到左边；否则吸附到右边
            float targetX = (x + view.getWidth() / 2) < centerX ? 0 : parent.getWidth() - view.getWidth();

            view.animate()
                    .x(targetX)
                    .y(y)
                    .setDuration(200)
                    .start();
        }
    }

    /**
     * 设置麦克风开闭
     */
    private void setMicFun() {
        if (!isCallActive || isCallEnded) {
            Log.w(TAG, "通话未激活或已结束，无法操作麦克风");
            return;
        }

        try {
            if (!isMicMuted) {
                // 麦克风禁音
                micManager.muteMic();
                Log.i(TAG, "麦克风已禁音");
            } else {
                // 麦克风打开
                micManager.unmuteMic();
                Log.i(TAG, "麦克风已打开");
            }
            isMicMuted = !isMicMuted;
            setMicImage();
        } catch (Exception e) {
            Log.e(TAG, "麦克风操作失败", e);
            Toast.makeText(this, "麦克风操作失败", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 设置外放开闭音
     */
    private void setVoiceFun() {
        if (!isCallActive || isCallEnded) {
            Log.w(TAG, "通话未激活或已结束，无法操作扬声器");
            return;
        }

        try {
            if (isVoiceOn) {
                originalVolume = voiceMuteManager.getCurrentMusicVolume(); // 保存原始音量
                voiceMuteManager.setMusicVolumeToZero(0);
                Log.i(TAG, "扬声器已关闭");
            } else {
                voiceMuteManager.setMusicVolumeToZero(originalVolume);
                Log.i(TAG, "扬声器已打开");
            }
            isVoiceOn = !isVoiceOn;
            setVoiceImage();
        } catch (Exception e) {
            Log.e(TAG, "扬声器操作失败", e);
            Toast.makeText(this, "扬声器操作失败", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 设置麦克风图片
     */
    private void setMicImage() {
        int imgId = isMicMuted ? R.mipmap.mic_off : R.mipmap.mic_on;
        micBtn.setImageResource(imgId);
    }

    /**
     * 设置是否禁播放音
     */
    private void setVoiceImage() {
        int imgId = isVoiceOn ? R.mipmap.music_on : R.mipmap.music_off;
        voiceBtn.setImageResource(imgId);
    }

    /**
     * 设置通话结束状态
     */
    private void setCallEnded() {
        isCallActive = false;
        isCallEnded = true;
        Log.i(TAG, "通话已结束");
    }

    /**
     * 创建连接通话
     */
    private void createLink() {
        if (callType == 1) {
            // 视频连接
            videoLink();
        } else {
            //音频连接
            voiceLink();
        }
    }

    /**
     * 音频连接
     */
    private void voiceLink() {
        if (PnasContactUtil.getInstance().isGroupNumber(accountString)) {
            PnasCallUtil.getInstance().makeCallWithOptions(accountString, false,
                    TrunkingCallSession.CallTypeDesc.CALLTYPE_VOICE_GROUP, false, false, false, 1);
        } else {
            PnasCallUtil.getInstance().makeCallWithOptions(accountString, false,
                    TrunkingCallSession.CallTypeDesc.CALLTYPE_FULL_DUPLEX_VOICE, false, false, false, 1);
        }
    }

    /**
     * 视频链接
     */
    private void videoLink() {
        if (PnasContactUtil.getInstance().isGroupNumber(accountString)) {
            PnasCallUtil.getInstance().makeCallWithOptions(accountString, false,
                    TrunkingCallSession.CallTypeDesc.CALLTYPE_VIDEO_GROUP, false, false, false, 1);
        } else {
            PnasCallUtil.getInstance().makeCallWithOptions(accountString, false,
                    TrunkingCallSession.CallTypeDesc.CALLTYPE_VIDEO, false, false, false, 1);
        }
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

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onStandbyGroupInfoChangedCallbackEvent(StandbyGroupInfoChangedCallbackEvent event) {
        if (event.getTrunkingGroupContact() != null) {
            Log.i(TAG, "守候组：" + event.getTrunkingGroupContact().getGroupName());
        } else {
            Log.i(TAG, "无守候组");
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        // 恢复原始音量
        if (voiceMuteManager != null) {
            voiceMuteManager.setMusicVolumeToZero(originalVolume);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 恢复麦克风状态
        if (micManager != null) {
            micManager.unmuteMic();
        }
        // 取消事件注册
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this);
        }
        // 移除双击检测的Handler回调
        if (doubleClickHandler != null) {
            doubleClickHandler.removeCallbacks(doubleClickRunnable);
            doubleClickHandler.removeCallbacksAndMessages(null);
        }
        Log.i(TAG, "MessageUiActivity 已销毁");
    }

    /**
     * 开始建立连接
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onOpenMessageEvent(OpenVideoActivityEvent event) {
        TrunkingCallSession callSession = event.getCallSession();
        if (Objects.isNull(callSession)) {
            return;
        }

        // 列表拨号时，对方接听
        if (comeType == 0) {
            Log.i(TAG, "对方已接听通话");
            isCallActive = true;
            // 可以在这里更新UI，显示通话已连接
        }
    }

    /**
     * 关闭连接 - 修复重复挂断问题
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onCloseMessageEvent(CloseVideoActivityEvent event) {
        TrunkingCallSession callSession = event.getCallSession();
        if (Objects.isNull(callSession)) {
            Log.w(TAG, "关闭事件：通话会话为空");
            // 只是结束界面，不重复挂断
            runOnUiThread(() -> {
                Toast.makeText(this, "通话已结束", Toast.LENGTH_SHORT).show();
                finish();
            });
            return;
        }

        if (callSession.isAfterEnded()) {
            Log.i(TAG, "关闭事件：通话已自然结束");
            setCallEnded();
            runOnUiThread(() -> {
                Toast.makeText(this, "通话已结束", Toast.LENGTH_SHORT).show();
                PnasCallUtil.getInstance().hangupActiveCall();
                loVideo.setVisibility(View.INVISIBLE);
                // 延迟一点时间再结束界面，让用户看到提示
                new android.os.Handler().postDelayed(this::finish, 1000);
            });
        } else {
            Log.w(TAG, "关闭事件：通话仍在进行中，忽略此事件");
            // 通话还在进行中，可能是误触发，不处理
        }
    }
}