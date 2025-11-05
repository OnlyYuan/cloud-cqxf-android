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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.mydemo.test31.R;

/**
 * 选择通话方式
 */
public class SelectPicDialog extends DialogFragment {
    private OnSelectPicDialogListener mListener;

    // 选择结果回调接口
    public interface OnSelectPicDialogListener {
        void onOptionSelected(int type);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // 加载布局
        View view = inflater.inflate(R.layout.dialog_select_pic,container, false);

        // 初始化视图
        TextView goCamera = view.findViewById(R.id.goCamera);
        TextView goAlbum = view.findViewById(R.id.goAlbum);
        TextView cancel = view.findViewById(R.id.cancel);

        // 设置点击事件
        goCamera.setOnClickListener(v -> {
            mListener.onOptionSelected(0);
            dismiss();
        });

        goAlbum.setOnClickListener(v -> {
            mListener.onOptionSelected(1);
            dismiss();
        });

        cancel.setOnClickListener(v -> {
            mListener.onOptionSelected(2);
            dismiss();
        });

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        // 设置弹窗属性
        Dialog dialog = getDialog();
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
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
    public void setLinkListener(OnSelectPicDialogListener listener) {
        mListener = listener;
    }
}