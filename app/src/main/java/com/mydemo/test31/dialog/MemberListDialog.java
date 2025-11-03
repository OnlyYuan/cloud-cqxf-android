
package com.mydemo.test31.dialog;

import android.app.Dialog;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mpttpnas.api.TrunkingGroupContact;
import com.mpttpnas.api.TrunkingLocalContact;
import com.mpttpnas.pnaslibraryapi.PnasContactUtil;
import com.mydemo.test31.R;
import com.mydemo.test31.adapter.MemberAdapter;

import java.util.List;

/**
 * 部门人员
 */
public class MemberListDialog extends DialogFragment {
    private String TAG = "MemberListDialog";
    private OnOptionSelectedListener mListener;
    private TrunkingGroupContact trunkingGroupContact;

    public MemberListDialog(TrunkingGroupContact mTrunkingGroupContact) {
        this.trunkingGroupContact = mTrunkingGroupContact;
    }

    // 选择结果回调接口
    public interface OnOptionSelectedListener {
        void onOptionSelected(TrunkingLocalContact item);
    }

    // 设置回调监听器
    public void setOnOptionSelectedListener(OnOptionSelectedListener listener) {
        mListener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // 加载弹窗布局
        View view = inflater.inflate(R.layout.unit_dialog, container, false);
        initView(view);
        return view;
    }

    private void initView(View view) {
        // 初始化RecyclerView
        RecyclerView rvOptions = view.findViewById(R.id.rv_options);
        rvOptions.setLayoutManager(new LinearLayoutManager(getContext()));

        // 模拟数据
        List<TrunkingLocalContact> optionList = getMemberList(trunkingGroupContact.getGroupGdn());

        // 设置适配器
        MemberAdapter adapter = new MemberAdapter(optionList, item -> {
            // 触发回调并关闭弹窗
            if (mListener != null) {
                mListener.onOptionSelected(item);
            }
            dismiss();
        });
        rvOptions.setAdapter(adapter);

        // 取消按钮
        TextView btnCancel = view.findViewById(R.id.btn_cancel);
        btnCancel.setOnClickListener(v -> dismiss());
    }

    // 设置弹窗属性
    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            Window window = dialog.getWindow();
            if (window != null) {
                // 底部显示 + 宽度全屏
                window.setGravity(Gravity.BOTTOM);
                window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
                // 动画效果
                window.setWindowAnimations(R.style.BottomSheetAnimation);
                // 背景透明
                window.setBackgroundDrawableResource(android.R.color.transparent);
            }
            // 点击外部可关闭
            dialog.setCanceledOnTouchOutside(true);
        }
    }

    /**
     * 获取成员列表
     *
     * @return groupGdn 组列表  list.get(0).getGroupGdn()
     */
    private List<TrunkingLocalContact> getMemberList(String groupGdn) {
        List<TrunkingLocalContact> memberList = PnasContactUtil
                .getInstance().getGroupContactList(groupGdn);
        for (int i = 0; i < memberList.size(); i++) {
            Log.d(TAG, "成员数据：" + memberList.get(i).getName());
        }
        return memberList;
    }
}