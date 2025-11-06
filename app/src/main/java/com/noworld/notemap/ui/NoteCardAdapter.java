package com.noworld.notemap.ui;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.noworld.notemap.R;
import com.amap.apis.cluster.demo.RegionItem;
import java.util.List;

// [新增] 图片加载库 (您需要在 build.gradle 中添加 Glide 依赖)
// implementation 'com.github.bumptech.glide:glide:4.12.0'
import com.bumptech.glide.Glide;

public class NoteCardAdapter extends RecyclerView.Adapter<NoteCardAdapter.ViewHolder> {

    private final Context mContext;
    private final List<RegionItem> mNotesList;

    public NoteCardAdapter(Context context, List<RegionItem> notesList) {
        this.mContext = context;
        this.mNotesList = notesList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // 加载卡片布局
        View view = LayoutInflater.from(mContext).inflate(R.layout.item_note_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RegionItem note = mNotesList.get(position);

        // 绑定数据
        holder.tvTitle.setText(note.getTitle());
        holder.tvAuthorName.setText(note.getAuthorName());
        holder.tvLikeCount.setText(String.valueOf(note.getLikeCount()));

        // [依赖 Member B] 加载图片 (使用 Glide)
        // 封面图 (占位符)
        Glide.with(mContext)
                .load(note.getPhotoUrl()) // Member B 需提供 URL
                .placeholder(R.drawable.ic_car) // 您的占位图
                .error(R.drawable.ic_car) // 加载失败图
                .into(holder.ivPhoto);

        // 作者头像 (占位符)
        Glide.with(mContext)
                .load(note.getAuthorAvatarUrl()) // Member B 需提供 URL
                .placeholder(R.drawable.ic_profile) // 您的占位图
                .error(R.drawable.ic_profile)
                .circleCrop() // 设置为圆形头像
                .into(holder.ivAuthorAvatar);

        // [核心交互] 设置卡片点击事件
        holder.itemView.setOnClickListener(v -> {

            // [修改] 启动 NoteDetailActivity (笔记正文页)
            Intent intent = new Intent(mContext, NoteDetailActivity.class);
            // 【关键】将整个笔记对象传递过去
            intent.putExtra(NoteDetailActivity.EXTRA_NOTE_DATA, note);
            mContext.startActivity(intent);

            // [删除] 临时提示
            // Toast.makeText(mContext, "点击了笔记: " + note.getTitle(), Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public int getItemCount() {
        return mNotesList.size();
    }

    // ViewHolder 类
    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivPhoto;
        TextView tvTitle;
        ImageView ivAuthorAvatar;
        TextView tvAuthorName;
        ImageView ivLikeIcon;
        TextView tvLikeCount;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPhoto = itemView.findViewById(R.id.iv_note_photo);
            tvTitle = itemView.findViewById(R.id.tv_note_title);
            ivAuthorAvatar = itemView.findViewById(R.id.iv_author_avatar);
            tvAuthorName = itemView.findViewById(R.id.tv_author_name);
            ivLikeIcon = itemView.findViewById(R.id.iv_like_icon);
            tvLikeCount = itemView.findViewById(R.id.tv_like_count);
        }
    }
}