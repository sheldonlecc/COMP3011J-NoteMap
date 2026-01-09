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
 * Image preview adapter for note publishing.
 */
public class ImagePreviewAdapter extends RecyclerView.Adapter<ImagePreviewAdapter.ImageViewHolder> {

    // Callback to notify the Activity when a delete is triggered.
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
        // Inflate item_image_preview.xml.
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_image_preview, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        Uri uri = imageUris.get(position);

        // 1. Load the selected image with Glide.
        Glide.with(holder.itemView.getContext())
                .load(uri)
                .centerCrop()
                .into(holder.ivImage);

        // 2. Handle the delete icon click.
        holder.ivDelete.setOnClickListener(v -> {
            if (deleteListener != null) {
                // Notify the Activity which image was removed.
                deleteListener.onDelete(holder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return imageUris.size();
    }

    // ViewHolder for binding XML views.
    static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage;
        ImageView ivDelete;

        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            // Bind IDs from item_image_preview.xml.
            ivImage = itemView.findViewById(R.id.iv_image);
            ivDelete = itemView.findViewById(R.id.iv_delete);
        }
    }
}
