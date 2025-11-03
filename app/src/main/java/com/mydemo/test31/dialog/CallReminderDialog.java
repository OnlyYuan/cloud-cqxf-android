package com.mydemo.test31.dialog;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.gms.common.util.CollectionUtils;
import com.mpttpnas.api.TrunkingCallSession;
import com.mpttpnas.api.TrunkingGroupContact;
import com.mpttpnas.api.TrunkingLocalContact;
import com.mpttpnas.pnaslibraryapi.PnasCallUtil;
import com.mpttpnas.pnaslibraryapi.PnasContactUtil;
import com.mydemo.test31.MessageUiActivity;
import com.mydemo.test31.R;

import java.util.List;

public class CallReminderDialog extends DialogFragment {

    private final TrunkingCallSession callSession;
    private TextView title_text;

    public CallReminderDialog(TrunkingCallSession callSession) {
        this.callSession = callSession;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        // 在创建对话框时设置无标题
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    /**
     * 创建对话框
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // 加载布局
        View view = inflater.inflate(R.layout.call_reminder_dialog, container, false);
        initView(view);
        return view;
    }

    // 3. 视图创建完成后
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        updateDisplayText();
    }

    /**
     * 更新显示文本
     */
    private void updateDisplayText() {
        if (title_text == null || callSession == null) return;

        List<TrunkingGroupContact> groupList = PnasContactUtil.getInstance().getMyGroupList();

        if (CollectionUtils.isEmpty(groupList)) {
            // 如果没有群组数据，显示基本信息
            setDefaultDisplayText();
            return;
        }

        String remoteContact = callSession.getRemoteContact(); // 使用getter方法
        String contactName = "";
        String groupName = "";

        // 遍历所有组和成员，找到对应的联系人和组
        for (TrunkingGroupContact groupContact : groupList) {
            List<TrunkingLocalContact> memberList = PnasContactUtil
                    .getInstance().getGroupContactList(groupContact.getGroupGdn());

            if (CollectionUtils.isEmpty(memberList)) {
                continue;
            }

            // 在当前组的成员中查找
            for (TrunkingLocalContact contact : memberList) {
                if (contact.getUdn().equals(remoteContact)) {
                    contactName = contact.getName();
                    groupName = groupContact.getGroupName();
                    break;
                }
            }

            if (!TextUtils.isEmpty(contactName)) {
                break; // 找到后就退出循环
            }
        }

        // 如果没有找到联系人，使用默认值
        if (TextUtils.isEmpty(contactName)) {
            contactName = remoteContact != null ? remoteContact : "未知联系人";
        }
        if (TextUtils.isEmpty(groupName)) {
            groupName = "未知组";
        }

        // 设置显示文本
        String callType = callSession.isVideoCall() ? "视频" : "语音";
        String displayText = String.format("%s | %s\n正在%s来电", contactName, groupName, callType);
        title_text.setText(displayText);
    }

    /**
     * 设置默认显示文本
     */
    private void setDefaultDisplayText() {
        if (title_text == null || callSession == null) return;

        String remoteContact = callSession.getRemoteContact();
        String contactName = remoteContact != null ? remoteContact : "未知联系人";
        String callType = callSession.isVideoCall() ? "视频" : "语音";
        String displayText = String.format("%s\n正在%s来电", contactName, callType);
        title_text.setText(displayText);
    }

    /**
     * 对话框开始显示（重要：设置对话框样式的最佳位置）
     */
    @Override
    public void onStart() {
        super.onStart();
        setupDialogStyle();
        // 确保文本内容是最新的
        updateDisplayText();
    }

    private void setupDialogStyle() {
        Dialog dialog = getDialog();
        if (dialog != null) {
            Window window = dialog.getWindow();
            if (window != null) {
                // 设置对话框属性
                WindowManager.LayoutParams params = window.getAttributes();

                // 方法1：设置固定宽度（推荐）
                int width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 300, getResources().getDisplayMetrics());
                params.width = width; // 固定300dp宽度

                // 方法2：或者使用屏幕宽度的百分比
                // DisplayMetrics displayMetrics = new DisplayMetrics();
                // requireActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
                // int screenWidth = displayMetrics.widthPixels;
                // params.width = (int) (screenWidth * 0.7); // 70%屏幕宽度

                params.height = WindowManager.LayoutParams.WRAP_CONTENT;
                params.gravity = Gravity.CENTER;

                // 设置动画（可选）
                // params.windowAnimations = R.style.DialogAnimation;

                window.setAttributes(params);

                // 设置背景透明，让圆角生效
                window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

                // 设置点击外部不消失
                dialog.setCanceledOnTouchOutside(false);

                // 移除阴影
                window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            }
        }
    }

    private void initView(View view) {
        title_text = view.findViewById(R.id.title_text);

        // 接听
        Button answer_btn = view.findViewById(R.id.answer_btn);
        answer_btn.setOnClickListener(btn -> {
            dismiss();
            Intent intent = new Intent(requireContext(), MessageUiActivity.class);
            intent.putExtra("comeType", 1);
            intent.putExtra("callSession", callSession);
            startActivity(intent);
        });

        // 挂断
        Button hang_up_btn = view.findViewById(R.id.hang_up_btn);
        hang_up_btn.setOnClickListener(btn -> {
            dismiss();
            // 点击取消后的逻辑
            PnasCallUtil.getInstance().hangupActiveCall();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // 清理引用
        title_text = null;
    }
}