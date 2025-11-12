package com.mydemo.test31.activity;

import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
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

    // 视频通话区域
    private FrameLayout loVideo = null;
    private SurfaceView renderView;
    private ConstraintLayout unLinkBg;

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
        renderView = new SurfaceView(this);

        initVoiceMute();
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        lp.gravity = Gravity.CENTER;
        renderView.setLayoutParams(lp);
        renderView.getHolder().addCallback(new VideoSurfaceCallback(PnasCallUtil.VideoCallWindow.LAUNCH_REMOTE_VIDEO));
        loVideo.addView(renderView, 0);
        renderView.setVisibility(View.VISIBLE);
        setMicImage();
        setVoiceImage();
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

        // 调节音量
        volumeBtn.setOnClickListener(view -> voiceMuteManager.raiseVolume());

        closeBtn.setOnClickListener(view -> {
            if (isCallActive && !isCallEnded) {
                Log.i(TAG, "用户主动挂断通话");
                PnasCallUtil.getInstance().hangupActiveCall();
                setCallEnded();
            }
            finish();
        });
    }

    /**
     * 设置麦克风开闭
     */
    private void setMicFun() {
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
    }

    /**
     * 设置外放开闭音
     */
    private void setVoiceFun() {
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
    }

    /**
     * 设置麦克风图片
     */
    private void setMicImage() {
        int imgId = R.mipmap.ic_launcher;
        if (isMicMuted) {
            imgId = R.mipmap.mic_off;
        } else {
            imgId = R.mipmap.mic_on;
        }
        micBtn.setImageResource(imgId);
    }

    /**
     * 设置是否禁播放音
     */
    private void setVoiceImage() {
        int imgId = R.mipmap.ic_launcher;
        if (isVoiceOn) {
            imgId = R.mipmap.music_on;
        } else {
            imgId = R.mipmap.music_off;
        }
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