package com.mydemo.test31.dialog;

import android.app.Dialog;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.DialogFragment;

import com.mydemo.test31.R;

public class LinkWayDialog extends DialogFragment {
    private OnLinkWaySelectedListener mListener;

    // 选择结果回调接口
    public interface OnLinkWaySelectedListener {
        void onOptionSelected(int position);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // 加载布局
        View view = inflater.inflate(R.layout.dialog_link_way,container, false);

        // 初始化视图
        ConstraintLayout voiceBtn = view.findViewById(R.id.voice_btn);
        ConstraintLayout movieBtn = view.findViewById(R.id.movie_btn);
        TextView cancel = view.findViewById(R.id.cancel);

        // 设置点击事件
        voiceBtn.setOnClickListener(v -> {
            mListener.onOptionSelected(0);
            Toast.makeText(getContext(), "点击了选项一", Toast.LENGTH_SHORT).show();
            dismiss();
        });

        movieBtn.setOnClickListener(v -> {
            mListener.onOptionSelected(1);
            Toast.makeText(getContext(), "点击了选项二", Toast.LENGTH_SHORT).show();
            dismiss();
        });

        cancel.setOnClickListener(v -> dismiss());

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        // 设置弹窗属性
        Dialog dialog = getDialog();
        if (dialog != null) {
            Window window = dialog.getWindow();
            if (window != null) {
                // 设置弹窗位置在底部
                window.setGravity(Gravity.BOTTOM);
                // 设置弹窗宽度为屏幕宽度
                window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
                // 设置动画效果
                window.setWindowAnimations(R.style.BottomSheetAnimation);
                // 设置背景透明
                window.setBackgroundDrawableResource(android.R.color.transparent);
            }
        }
    }

    // 设置回调监听器
    public void setLinkListener(OnLinkWaySelectedListener listener) {
        mListener = listener;
    }
}