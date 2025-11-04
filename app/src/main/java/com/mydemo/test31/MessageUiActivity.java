package com.mydemo.test31;

import android.app.AlertDialog;
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
import com.mpttpnas.api.TrunkingConversation;
import com.mpttpnas.api.TrunkingMessage;
import com.mpttpnas.pnas.agent.PnasErrorCode;
import com.mpttpnas.pnaslibraryapi.PnasCallUtil;
import com.mpttpnas.pnaslibraryapi.PnasContactUtil;
import com.mpttpnas.pnaslibraryapi.callback.FloorStateChangedCallbackEvent;
import com.mpttpnas.pnaslibraryapi.callback.GroupAffiliactionNotifyResultCallbackEvent;
import com.mpttpnas.pnaslibraryapi.callback.StandbyGroupInfoChangedCallbackEvent;
import com.mydemo.test31.dialog.CallReminderDialog;
import com.mydemo.test31.event.CloseVideoActivityEvent;
import com.mydemo.test31.event.OpenVideoActivityEvent;
import com.mydemo.test31.util.MicMuteManager;
import com.mydemo.test31.util.MuteManager;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Objects;

public class MessageUiActivity extends AppCompatActivity {

    private String TAG = "MessageUiActivity";
    //连接对象的账户名
    private String accountString = "";
    //连接
    private Button connectBtn = null;
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
    private AlertDialog mDialog = null;
    //等待对方接听的等待弹窗
    private AlertDialog connecttingDialog = null;
    private int callType = 0;//创建连接的方式 0.语音 1.视频
    private int comeType = 0;//进入方式方式，0.根据列表进入 1.接收电话进入

    // 声音开关状态  true是开  false是关
    private boolean isVoiceOn = true;

    // mic的状态  true是禁音，false未禁音
    private boolean isMicMuted = false;

    //mic禁音
    MicMuteManager micManager = null;
    //声音禁音
    MuteManager voiceMuteManager = null;

    // 用于保存原始音量，以便恢复
    private int originalVolume = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message_ui);
        initData();
        initView();
        initListener();
    }

    private void initData() {
        accountString = getIntent().getStringExtra("account");
        callType = getIntent().getIntExtra("callType", 0);
        comeType = getIntent().getIntExtra("comeType", 0);
        if (comeType == 1) {
            callSession = getIntent().getParcelableExtra("callSession");
            accountString = callSession.getGroupCallUdn();
            Log.i(TAG, "-->account getRemoteContact:" + callSession.getRemoteContact()
                    + "getCallState:" + callSession.getCallState() + "name"
            );
            accountString = callSession.getRemoteContact();
            acceptCall();
        }
        EventBus.getDefault().register(this);
        Log.i(TAG, "--->account:" + accountString + "callType:" + callType + "comeType" + comeType);
        // 在 Activity 中使用
        micManager = new MicMuteManager(this);
        isMicMuted = micManager.isMicMuted();
    }


    private void initView() {
        connectBtn = findViewById(R.id.connectBtn);
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
        connectShow(false);
        setMicImage();
        setVoiceImage();
    }

    private void initVoiceMute() {
        voiceMuteManager = new MuteManager(this);
        originalVolume = voiceMuteManager.getCurrentMusicVolume(); // 保存原始音量
    }

    /**
     * 连接和取消按钮的显示隐藏
     */
    private void connectShow(Boolean isConnect) {
        if (isConnect) {
            unLinkBg.setVisibility(View.GONE);
            connectBtn.setVisibility(View.GONE);
            cancelBtn.setVisibility(View.VISIBLE);
        } else {
            unLinkBg.setVisibility(View.VISIBLE);
            connectBtn.setVisibility(View.VISIBLE);
            cancelBtn.setVisibility(View.GONE);
        }
    }

    private void initListener() {
        connectBtn.setOnClickListener(view -> {
            connectShow(true);
            connectDialog();
            createLink();
        });

        cancelBtn.setOnClickListener(v -> {
            // 挂断
            PnasCallUtil.getInstance().hangupActiveCall();
            connectShow(false);
        });

        //禁音
        voiceBtn.setOnClickListener(v -> {
            setVoiceFun();
        });

        //麦克风
        micBtn.setOnClickListener(v ->
                setMicFun()
        );

        //调节音量
        volumeBtn.setOnClickListener(v ->
                voiceMuteManager.raiseVolume()
        );

        closeBtn.setOnClickListener(v -> {
            PnasCallUtil.getInstance().hangupActiveCall();
            MessageUiActivity.this.finish();
        });

    }

    /**
     * 设置麦克风开闭
     */
    private void setMicFun() {
        if (!isMicMuted) {
            // 麦克风禁音
            micManager.muteMic();
        } else {
            // 麦克风打开
            micManager.unmuteMic();
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
        } else {
            voiceMuteManager.setMusicVolumeToZero(originalVolume);
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


    private void setVoiceNumFun() {

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

    TrunkingCallSession callSession;


    //话权变化回调
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onFloorStateChangedCallbackEvent(FloorStateChangedCallbackEvent event) {
        Log.d("onFloorState", "" + event.getCallSession().getIsinfo());
    }

    //话权变化回调
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
        TrunkingConversation conversation;
        TrunkingMessage message;
        PnasErrorCode errorCode;
        // PnasContactUtil.getInstance().getAllUserContactList()
    }

    /**
     * 接收链接
     */
    private void acceptCall() {
        if (callSession != null && callSession.isIncoming() && callSession.isBeforeConfirmed()) {
            PnasCallUtil.getInstance().acceptCall(callSession.getCallId());
        }
    }

    private void connectDialog() {
        if (Objects.nonNull(connecttingDialog)) {
            return;
        }
        // 创建对话框构建器
        connecttingDialog = new AlertDialog
                .Builder(this)
                .setTitle("连接中")
                .setMessage("等待对方接听...")
                .create();
        connecttingDialog.setCanceledOnTouchOutside(true);
        connecttingDialog.show();
    }

    private void dismissConnectDialog() {
        if (Objects.nonNull(connecttingDialog)) {
            connecttingDialog.dismiss();
            connecttingDialog = null;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        voiceMuteManager.setMusicVolumeToZero(originalVolume);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (micManager != null) {
            micManager.unmuteMic();
        }
        EventBus.getDefault().unregister(this);
    }

    /**
     * 开始建立连接
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onOpenMessageEvent(OpenVideoActivityEvent event) {
        callSession = event.getCallSession();
        if (Objects.isNull(callSession)) {
            return;
        }

        if (!callSession.isAfterEnded() && (callSession.getCallState() == 4
                || callSession.getCallState() == 5)) {
            CallReminderDialog reminderDialog = event.getCallReminderDialog();
            if (Objects.nonNull(reminderDialog)) {
                reminderDialog.dismiss();
            }
            dismissConnectDialog();
            connectShow(true);
            if (callSession.isVideoCall()) {
                //  视频通话
                loVideo.setVisibility(View.VISIBLE);
            }
        }
    }

    /**
     * 关闭连接
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onCloseMessageEvent(CloseVideoActivityEvent event) {
        callSession = event.getCallSession();
        if (Objects.isNull(callSession) || callSession.isAfterEnded()) {
            Log.i(TAG, "挂断了");
            Toast.makeText(this, "已挂断", Toast.LENGTH_SHORT).show();
            loVideo.setVisibility(View.INVISIBLE);
            dismissConnectDialog();
            connectShow(false);
            finish();
        }
    }

}