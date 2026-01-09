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

    public interface OnLikeChangeListener {
        void onUnlike(RegionItem note);
    }

    private final Context mContext;
    private final List<RegionItem> mNotesList;
    private final AliNoteRepository noteRepository;
    private final LikedStore likedStore;
    private final UserStore userStore;
    private final TokenStore tokenStore;
    private final OnLikeChangeListener onLikeChangeListener;

    public NoteCardAdapter(Context context, List<RegionItem> notesList) {
        this(context, notesList, null);
    }

    public NoteCardAdapter(Context context, List<RegionItem> notesList, OnLikeChangeListener listener) {
        this.mContext = context;
        this.mNotesList = notesList;
        this.noteRepository = AliNoteRepository.getInstance(context);
        this.likedStore = LikedStore.getInstance(context);
        this.userStore = UserStore.getInstance(context);
        this.tokenStore = TokenStore.getInstance(context);
        this.onLikeChangeListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the card layout.
        View view = LayoutInflater.from(mContext).inflate(R.layout.item_note_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RegionItem note = mNotesList.get(position);

        // Bind data.
        holder.tvTitle.setText(note.getTitle());
        holder.tvAuthorName.setText(note.getAuthorName());
        holder.tvLikeCount.setText(String.valueOf(resolveLikeCount(note)));
        ensureLikeStateFromStore(note);
        updateLikeIcon(holder, note);

        // Load images with Glide.
        // Cover image (placeholder).
        Glide.with(mContext)
                .load(buildOssModel(note.getPhotoUrl())) // URL provided by backend.
                .placeholder(R.drawable.ic_car) // Placeholder.
                .error(R.drawable.ic_car) // Error fallback.
                .into(holder.ivPhoto);

        // Author avatar (placeholder).
        Glide.with(mContext)
                .load(buildOssModel(note.getAuthorAvatarUrl())) // URL provided by backend.
                .placeholder(R.drawable.ic_profile) // Placeholder.
                .error(R.drawable.ic_profile)
                .circleCrop() // Circle avatar.
                .into(holder.ivAuthorAvatar);

        // Core interaction: open note details on card tap.
        holder.itemView.setOnClickListener(v -> {

            // Launch NoteDetailActivity.
            Intent intent = new Intent(mContext, NoteDetailActivity.class);
            // Pass the full note object.
            intent.putExtra(NoteDetailActivity.EXTRA_NOTE_DATA, note);
            mContext.startActivity(intent);

            // Removed debug toast.
            // Toast.makeText(mContext, "Clicked note: " + note.getTitle(), Toast.LENGTH_SHORT).show();
        });

        holder.ivAuthorAvatar.setOnClickListener(v -> openProfile(note));
        holder.tvAuthorName.setOnClickListener(v -> openProfile(note));

        holder.ivLikeIcon.setOnClickListener(v -> handleLikeClicked(holder, note));
    }

    @Override
    public int getItemCount() {
        return mNotesList.size();
    }

    // ViewHolder class.
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
                // Update UI immediately to avoid full list refresh.
                holder.tvLikeCount.setText(String.valueOf(likeCount));
                int position = holder.getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    notifyItemChanged(position);
                }
                if (!liked && onLikeChangeListener != null) {
                    onLikeChangeListener.onUnlike(note);
                }
            }

            @Override
            public void onRequireLogin() {
                Toast.makeText(mContext, "Please log in before liking", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(mContext, LoginActivity.class);
                mContext.startActivity(intent);
            }

            @Override
            public void onError(@NonNull Throwable e) {
                Toast.makeText(mContext, "Like failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
        return Math.max(note.getLikeCount(), 0);
    }

    private void openProfile(RegionItem note) {
        Intent intent = new Intent(mContext, ProfileActivity.class);
        intent.putExtra(ProfileActivity.EXTRA_USER_ID, note.getAuthorId());
        intent.putExtra(ProfileActivity.EXTRA_USER_NAME, note.getAuthorName());
        intent.putExtra(ProfileActivity.EXTRA_USER_AVATAR, note.getAuthorAvatarUrl());
        mContext.startActivity(intent);
    }

    private String getCurrentUid() {
        String tokenUid = userStore.extractUidFromToken(tokenStore.getToken());
        if (tokenUid != null && !tokenUid.isEmpty()) return tokenUid;
        String uid = userStore.getUid();
        if (uid != null && !uid.isEmpty()) return uid;
        return "guest";
    }
}
