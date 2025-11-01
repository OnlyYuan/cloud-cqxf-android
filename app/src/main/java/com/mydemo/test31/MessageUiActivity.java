package com.mydemo.test31;

import android.app.AlertDialog;
import android.content.DialogInterface;
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

import com.mpttpnas.api.TrunkingCallSession;
import com.mpttpnas.api.TrunkingConversation;
import com.mpttpnas.api.TrunkingMessage;
import com.mpttpnas.pnas.agent.PnasErrorCode;
import com.mpttpnas.pnaslibraryapi.PnasCallUtil;
import com.mpttpnas.pnaslibraryapi.PnasContactUtil;
import com.mpttpnas.pnaslibraryapi.callback.CallStateChangedCallbackEvent;
import com.mpttpnas.pnaslibraryapi.callback.FloorStateChangedCallbackEvent;
import com.mpttpnas.pnaslibraryapi.callback.GroupAffiliactionNotifyResultCallbackEvent;
import com.mpttpnas.pnaslibraryapi.callback.StandbyGroupInfoChangedCallbackEvent;
import com.mydemo.test31.util.MicMuteManager;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class MessageUiActivity extends AppCompatActivity {

    private String TAG = "MessageUiActivity";
    //连接对象的账户名
    private String accountString = "";
    //连接
    private Button connectBtn =null;
    //断开
    private Button cancelBtn =null;
    //麦克风
    private ImageView micBtn =null;
    //音量
    private ImageView voiceBtn =null;
    private ImageView closeBtn =null;

    //视频通话区域
    private FrameLayout loVideo =null;
    private SurfaceView renderView;
    private  AlertDialog mDialog = null;
    private int callType = 0;//创建连接的方式 0.语音 1.视频
    private int comeType = 0;//进入方式方式，0.根据列表进入 1.接收电话进入

    //声音开关状态  true是开  false是关
    private boolean isVoiceOn = true;

    //mic的状态  true是禁音，false未禁音
    private boolean isMicMuted = true;
    // 在 Activity 中使用
    MicMuteManager micManager =null;

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
        callType = getIntent().getIntExtra("callType",0);
        comeType = getIntent().getIntExtra("comeType",0);
        if (comeType==1){
            callSession =  getIntent().getParcelableExtra("callSession");
            accountString =callSession.getGroupCallUdn();
            Log.i(TAG,"-->account getRemoteContact:"+callSession.getRemoteContact()
                +"getCallState:"+callSession.getCallState() +"name"
            );
            accountString = callSession.getRemoteContact();

            acceptCall();
        }
        EventBus.getDefault().register(this);
        Log.i(TAG,"--->account:"+accountString +"callType:" +callType +"comeType" +comeType);
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
        renderView = new SurfaceView(this);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        lp.gravity = Gravity.CENTER;
        renderView.setLayoutParams(lp);
        renderView.getHolder().addCallback(new VideoSurfaceCallback(PnasCallUtil.VideoCallWindow.LAUNCH_REMOTE_VIDEO));
        loVideo.addView(renderView, 0);
        renderView.setVisibility(View.VISIBLE);
        connectShow(false);
    }

    /**
     * 连接和取消按钮的显示隐藏
     * @param isConnect
     */
    private void connectShow(Boolean isConnect){
        if (isConnect){
            connectBtn.setVisibility(View.GONE);
            cancelBtn.setVisibility(View.VISIBLE);
        }else{
            connectBtn.setVisibility(View.VISIBLE);
            cancelBtn.setVisibility(View.GONE);
        }
    }
    private void initListener() {

        connectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createLink();
            }
        });

        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 挂断
                PnasCallUtil.getInstance().hangupActiveCall();
                connectShow(false);
            }
        });

        voiceBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        micBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setMicFun();
            }
        });

        closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MessageUiActivity.this.finish();
            }
        });

    }

    private void setMicFun(){

        if (isMicMuted){
            //麦克风禁音
            micManager.muteMic();
        }else {
            //麦克风打开
            micManager.unmuteMic();

        }

    }

    private void setVoiceNumFun(){

    }

    /**
     * 创建连接通话
     */
    private  void createLink(){
        if (callType == 1){//视频连接
            videoLink();
        }else {//音频连接
            voiceLink();
        }
    }

    /**
     * 音频连接
     */
    private void voiceLink(){
        if(PnasContactUtil.getInstance().isGroupNumber(accountString)){
            PnasCallUtil.getInstance().makeCallWithOptions(accountString,false, TrunkingCallSession.CallTypeDesc.CALLTYPE_VOICE_GROUP,false,false,false,1);
        }else {
            PnasCallUtil.getInstance().makeCallWithOptions(accountString, false, TrunkingCallSession.CallTypeDesc.CALLTYPE_FULL_DUPLEX_VOICE, false, false, false, 1);
        }
    }

    /**
     * 视频链接
     */
    private void videoLink(){
        if(PnasContactUtil.getInstance().isGroupNumber(accountString)){
            PnasCallUtil.getInstance().makeCallWithOptions(accountString,false, TrunkingCallSession.CallTypeDesc.CALLTYPE_VIDEO_GROUP,false,false,false,1);
        }else{
            PnasCallUtil.getInstance().makeCallWithOptions(accountString,false,TrunkingCallSession.CallTypeDesc.CALLTYPE_VIDEO,false,false,false,1);
        }
    }

    TrunkingCallSession callSession;
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onCallStateChangedCallbackEvent(CallStateChangedCallbackEvent event){
        callSession = event.getCallSession();
        if(callSession != null && !callSession.isAfterEnded()){
            if(callSession.isIncoming() && callSession.isBeforeConfirmed()){
                Log.i(TAG,"--->还未接听");
                acceptDialog();
            }else{
                if (mDialog!=null){
                    mDialog.dismiss();
                }
                mDialog =null;
                connectShow(true);
                if(callSession.isVideoCall()){//视频通话
                    loVideo.setVisibility(View.VISIBLE);
                }
            }
        }else{
            Log.i(TAG,"挂断了");
            mDialog.dismiss();
            mDialog =null;
            Toast.makeText(this,"已挂断",Toast.LENGTH_SHORT).show();
            connectShow(false);
        }
    }

    //话权变化回调
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onFloorStateChangedCallbackEvent(FloorStateChangedCallbackEvent event) {
        Log.d("onFloorState","" + event.getCallSession().getIsinfo());
    }

    //话权变化回调
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onGroupAffiliactionNotifyResultCallbackEvent(GroupAffiliactionNotifyResultCallbackEvent event) {
        Log.d("GroupAffi","" + event.getGroupNumber() + "," + event.isSuccess());
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onStandbyGroupInfoChangedCallbackEvent(StandbyGroupInfoChangedCallbackEvent event){
        if(event.getTrunkingGroupContact() != null) {
            Log.i(TAG,"守候组：" + event.getTrunkingGroupContact().getGroupName());
        }else{
            Log.i(TAG,"无守候组");
        }
        TrunkingConversation conversation;
        TrunkingMessage message;
        PnasErrorCode errorCode;
//        PnasContactUtil.getInstance().getAllUserContactList()
    }

    /**
     * 接收链接
     */
    private void acceptCall(){
        if(callSession!=null && callSession.isIncoming() && callSession.isBeforeConfirmed()) {
            PnasCallUtil.getInstance().acceptCall(callSession.getCallId());
        }
    }

    /**
     * 来电弹窗
     */
    private void acceptDialog() {

        String title = "";

        if(callSession.isVideoCall()){
            title =   "视频来电";
        }else{
            title =  "语音来电";
        }
        if (mDialog !=null){
            return;
        }
        // 创建对话框构建器
        mDialog = new AlertDialog
                .Builder(this)
                .setTitle("来电提示")
                .setMessage(title)
                .setPositiveButton("接听", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // 点击确定后的逻辑
                        acceptCall();
                        dialog.dismiss(); // 关闭对话框
                    }
                })
                .setNegativeButton("挂断", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // 点击取消后的逻辑
                        PnasCallUtil.getInstance().hangupActiveCall();
                        dialog.dismiss();
                    }
                }).create();
        mDialog.setCanceledOnTouchOutside(false);
        mDialog.show();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(micManager!=null){
            micManager.unmuteMic();
        }
        EventBus.getDefault().unregister(this);
    }


}