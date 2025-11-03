package com.mydemo.test31.dialog;


import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.TextUtils;
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

    private final String text = "环线号线-陈家坪 | 客运中心" +
            "\n正在视频来电";

    public CallReminderDialog(TrunkingCallSession callSession) {
        this.callSession = callSession;
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

        TextView title_text = view.findViewById(R.id.title_text);
        List<TrunkingGroupContact> groupList = PnasContactUtil.getInstance().getMyGroupList();

        if (CollectionUtils.isEmpty(groupList)) {
            return;
        }

        String remoteContact = callSession.remoteContact;
        String contactName = "";
        String groupName = "";

        // 遍历所有组和成员，找到对应的联系人和组
        for (TrunkingGroupContact groupContact : groupList) {
            List<TrunkingLocalContact> memberList = PnasContactUtil
                    .getInstance().getGroupContactList(groupContact.getGroupGdn());

            // 在当前组的成员中查找
            TrunkingLocalContact contact = memberList.stream()
                    .filter(item -> item.getUdn().equals(remoteContact))
                    .findFirst()
                    .orElse(null);

            if (contact != null) {
                contactName = contact.getName();
                groupName = groupContact.getGroupName(); // 获取组名
                break; // 找到后就退出循环
            }
        }

        // 如果没有找到联系人，使用默认值
        if (TextUtils.isEmpty(contactName)) {
            contactName = "未知联系人";
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
     * 对话框开始显示（重要：设置对话框样式的最佳位置）
     */
    @Override
    public void onStart() {
        super.onStart();
        setupDialogStyle();
    }

    // 8. 销毁视图
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // 清理与视图相关的资源
    }

    // 9. 完全销毁
    @Override
    public void onDestroy() {
        super.onDestroy();
        // 清理所有资源
    }


    private void setupDialogStyle() {
        Window window = getDialog().getWindow();
        if (window != null) {
            // 设置窗口圆角
            window.setClipToOutline(true);
            // 如果需要，可以设置窗口的圆角
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                window.setBackgroundDrawableResource(R.drawable.dialog_background);
            } else {
                // 设置背景透明，让圆角可见
                window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            }
            // 移除阴影
            window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        }
    }

    private void initView(View view) {
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
}
