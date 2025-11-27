package com.noworld.notemap.ui.adapter;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.noworld.notemap.R;

import java.util.List;

/**
 * 发布笔记时的图片预览适配器
 */
public class ImagePreviewAdapter extends RecyclerView.Adapter<ImagePreviewAdapter.ImageViewHolder> {

    // 定义一个回调接口，当点击删除时通知 Activity
    public interface OnDeleteClickListener {
        void onDelete(int position);
    }

    private final List<Uri> imageUris;
    private final OnDeleteClickListener deleteListener;

    public ImagePreviewAdapter(List<Uri> imageUris, OnDeleteClickListener deleteListener) {
        this.imageUris = imageUris;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // 这里加载我们在上一步修改好的 item_image_preview.xml
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_image_preview, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        Uri uri = imageUris.get(position);

        // 1. 使用 Glide 加载您选中的图片
        Glide.with(holder.itemView.getContext())
                .load(uri)
                .centerCrop()
                .into(holder.ivImage);

        // 2. 设置红色叉号的点击事件
        holder.ivDelete.setOnClickListener(v -> {
            if (deleteListener != null) {
                // 通知 Activity：第几个图片被删了
                deleteListener.onDelete(holder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return imageUris.size();
    }

    // ViewHolder 类，绑定 XML 里的控件
    static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage;
        ImageView ivDelete;

        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            // 绑定 item_image_preview.xml 里的 ID
            ivImage = itemView.findViewById(R.id.iv_image);
            ivDelete = itemView.findViewById(R.id.iv_delete);
        }
    }
}