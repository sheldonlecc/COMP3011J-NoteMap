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

import com.amap.apis.cluster.demo.RegionItem;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.noworld.notemap.R;
import com.noworld.notemap.data.AliNoteRepository;
import com.noworld.notemap.data.LikedStore;
import com.noworld.notemap.data.TokenStore;
import com.noworld.notemap.data.UserStore;

import java.util.List;
import java.util.Set;

public class NoteCardAdapter extends RecyclerView.Adapter<NoteCardAdapter.ViewHolder> {

    private final Context mContext;
    private final List<RegionItem> mNotesList;
    private final AliNoteRepository noteRepository;
    private final LikedStore likedStore;
    private final UserStore userStore;
    private final TokenStore tokenStore;

    public NoteCardAdapter(Context context, List<RegionItem> notesList) {
        this.mContext = context;
        this.mNotesList = notesList;
        this.noteRepository = AliNoteRepository.getInstance(context);
        this.likedStore = LikedStore.getInstance(context);
        this.userStore = UserStore.getInstance(context);
        this.tokenStore = TokenStore.getInstance(context);
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
        holder.tvLikeCount.setText(String.valueOf(resolveLikeCount(note)));
        ensureLikeStateFromStore(note);
        updateLikeIcon(holder, note);

        // [依赖 Member B] 加载图片 (使用 Glide)
        // 封面图 (占位符)
        Glide.with(mContext)
                .load(buildOssModel(note.getPhotoUrl())) // Member B 需提供 URL
                .placeholder(R.drawable.ic_car) // 您的占位图
                .error(R.drawable.ic_car) // 加载失败图
                .into(holder.ivPhoto);

        // 作者头像 (占位符)
        Glide.with(mContext)
                .load(buildOssModel(note.getAuthorAvatarUrl())) // Member B 需提供 URL
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

        holder.ivLikeIcon.setOnClickListener(v -> handleLikeClicked(holder, note));
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

    private void updateLikeIcon(ViewHolder holder, RegionItem note) {
        holder.ivLikeIcon.setImageResource(
                note.isLikedByCurrentUser() ? R.drawable.ic_like_filled : R.drawable.ic_like
        );
    }

    private void handleLikeClicked(ViewHolder holder, RegionItem note) {
        noteRepository.toggleLike(note, new AliNoteRepository.LikeCallback() {
            @Override
            public void onResult(boolean liked, int likeCount) {
                note.setLikedByCurrentUser(liked);
                note.setLikeCount(likeCount);
                likedStore.toggle(getCurrentUid(), note.getNoteId(), liked);
                likedStore.saveLikeCount(note.getNoteId(), likeCount);
                // 立即同步 UI 文本，避免需要完整刷新列表
                holder.tvLikeCount.setText(String.valueOf(likeCount));
                int position = holder.getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    notifyItemChanged(position);
                }
            }

            @Override
            public void onRequireLogin() {
                Toast.makeText(mContext, "请先登录后再点赞", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(mContext, LoginActivity.class);
                mContext.startActivity(intent);
            }

            @Override
            public void onError(@NonNull Throwable e) {
                Toast.makeText(mContext, "点赞失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private Object buildOssModel(String url) {
        if (url == null || url.isEmpty()) return url;
        String normalized = normalizeOssUrl(url);
        return new GlideUrl(normalized, new LazyHeaders.Builder()
                .addHeader("Referer", "http://notemap-prod-oss.oss-cn-beijing.aliyuncs.com")
                .build());
    }

    private String normalizeOssUrl(String url) {
        if (url == null) return null;
        if (url.startsWith("https://notemap-prod-oss.oss-cn-beijing.aliyuncs.com")) {
            return url.replaceFirst("^https://", "http://");
        }
        return url;
    }

    private void ensureLikeStateFromStore(RegionItem note) {
        if (note == null || note.getNoteId() == null) return;
        Set<String> liked = likedStore.getLikedIds(getCurrentUid());
        if (liked.contains(note.getNoteId()) && !note.isLikedByCurrentUser()) {
            note.setLikedByCurrentUser(true);
        }
    }

    private int resolveLikeCount(RegionItem note) {
        if (note == null) return 0;
        Integer stored = likedStore.getLikeCount(note.getNoteId());
        if (stored != null) {
            note.setLikeCount(stored);
            return stored;
        }
        return note.getLikeCount();
    }

    private String getCurrentUid() {
        String tokenUid = userStore.extractUidFromToken(tokenStore.getToken());
        if (tokenUid != null && !tokenUid.isEmpty()) return tokenUid;
        String uid = userStore.getUid();
        if (uid != null && !uid.isEmpty()) return uid;
        return "guest";
    }
}
