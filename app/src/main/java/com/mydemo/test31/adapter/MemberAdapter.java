package com.mydemo.test31.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.mpttpnas.api.TrunkingLocalContact;
import com.mydemo.test31.R;

import java.util.List;

public class MemberAdapter extends RecyclerView.Adapter<MemberAdapter.ViewHolder> {

    private final List<TrunkingLocalContact> mOptionList;
    private final OnItemClickListener mListener;

    // 点击事件接口
    public interface OnItemClickListener {
        void onItemClick(TrunkingLocalContact item);
    }

    // 构造方法
    public MemberAdapter(List<TrunkingLocalContact> optionList, OnItemClickListener listener) {
        mOptionList = optionList;
        mListener = listener;
    }

    // 视图持有者
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        ImageView ivArrow;

        public ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_option);

        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // 加载列表项布局
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_dialog, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        // 绑定数据
        TrunkingLocalContact item = mOptionList.get(position);
        holder.tvName.setText(item.getName());

        // 如需显示箭头，可打开此行
        // holder.ivArrow.setVisibility(View.VISIBLE);

        // 设置点击事件
        holder.itemView.setOnClickListener(v -> {
            if (mListener != null) {
                mListener.onItemClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mOptionList.size();
    }
}