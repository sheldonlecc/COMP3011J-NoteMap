package com.noworld.notemap.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView; // 1. 导入 ImageView
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide; // 2. 导入 Glide
import com.noworld.notemap.R;
import com.noworld.notemap.data.model.CommentItem;
import java.util.ArrayList;
import java.util.List;

public class CommentAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_COMMENT = 0;
    private static final int TYPE_MORE = 1;

    private final List<CommentItem> data;
    private OnCommentActionListener listener;

    public CommentAdapter(List<CommentItem> data) {
        this.data = data;
    }

    public void setOnCommentActionListener(OnCommentActionListener listener) {
        this.listener = listener;
    }

    public void updateData(List<CommentItem> newData) {
        List<CommentItem> source = newData;
        if (source == data) {
            source = new ArrayList<>(newData);
        }
        data.clear();
        if (source != null) {
            data.addAll(source);
        }
        notifyDataSetChanged();
    }

    public void addCommentToTop(CommentItem item) {
        if (item == null) return;
        data.add(0, item);
        notifyItemInserted(0);
    }

    public void addReplyAfterParent(CommentItem reply) {
        if (reply == null) return;
        if (!reply.isReply()) {
            addCommentToTop(reply);
            return;
        }
        int parentIndex = -1;
        for (int i = 0; i < data.size(); i++) {
            if (reply.getParentId() != null && reply.getParentId().equals(data.get(i).getId())) {
                parentIndex = i;
                break;
            }
        }
        if (parentIndex >= 0) {
            int insertPos = Math.min(parentIndex + 1, data.size());
            data.add(insertPos, reply);
            notifyItemInserted(insertPos);
        } else {
            addCommentToTop(reply);
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_MORE) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_comment_more, parent, false);
            return new MoreVH(v);
        }
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_comment, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        CommentItem item = data.get(position);
        android.util.Log.d("CommentAdapter", "bind pos=" + position + " type=" + getItemViewType(position) + " id=" + item.getId() + " parent=" + item.getParentId() + " remaining=" + item.getRemainingCount());
        if (holder instanceof MoreVH) {
            MoreVH h = (MoreVH) holder;
            String text = "显示更多评论";
            if (item.getRemainingCount() > 0) {
                text = "显示更多评论 (" + item.getRemainingCount() + ")";
            }
            h.tvMore.setText(text);
            h.itemView.setOnClickListener(v -> {
                if (listener != null) listener.onToggleReplies(item.getParentId(), true);
            });
            return;
        }
        VH h = (VH) holder;
        h.tvUser.setText(item.getUserName());
        if (item.getReplyToUserName() != null && !item.getReplyToUserName().isEmpty()) {
            h.tvContent.setText("回复 " + item.getReplyToUserName() + "：" + item.getContent());
        } else {
            h.tvContent.setText(item.getContent());
        }
        h.tvTime.setText(item.getTime());

        ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) h.itemView.getLayoutParams();
        if (lp != null) {
            int indent = 0;
            if (item.isReply()) {
                float density = h.itemView.getResources().getDisplayMetrics().density;
                indent = (int) (16 * density);
            }
            lp.leftMargin = indent;
            h.itemView.setLayoutParams(lp);
        }

        Glide.with(h.itemView.getContext())
                .load(item.getAvatarUrl() != null ? item.getAvatarUrl() : R.drawable.ic_profile)
                .placeholder(R.drawable.ic_profile)
                .error(R.drawable.ic_profile)
                .circleCrop()
                .into(h.ivAvatar);

        h.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onReply(item);
            }
        });
        h.layoutLike.setOnClickListener(v -> {
            if (listener != null) listener.onLike(item);
        });
        h.ivLike.setImageResource(item.isLiked() ? R.drawable.ic_like_filled : R.drawable.ic_like);
        int color = item.isLiked() ? 0xFFFF6E79 : 0xFF999999; // 红/灰
        h.ivLike.setColorFilter(color);
        h.tvLikeCount.setText(String.valueOf(item.getLikeCount()));
    }

    @Override
    public int getItemCount() { return data.size(); }

    @Override
    public int getItemViewType(int position) {
        return data.get(position).isMoreIndicator() ? TYPE_MORE : TYPE_COMMENT;
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvUser, tvContent, tvTime, tvLikeCount;
        ImageView ivAvatar, ivLike; // 4. 声明 ImageView
        View layoutLike;

        VH(View v) {
            super(v);
            tvUser = v.findViewById(R.id.tv_comment_user);
            tvContent = v.findViewById(R.id.tv_comment_content);
            tvTime = v.findViewById(R.id.tv_comment_time);
            tvLikeCount = v.findViewById(R.id.tv_comment_like_count);
            ivAvatar = v.findViewById(R.id.iv_comment_avatar);
            ivLike = v.findViewById(R.id.iv_comment_like);
            layoutLike = v.findViewById(R.id.layout_comment_like);
        }
    }

    static class MoreVH extends RecyclerView.ViewHolder {
        TextView tvMore;
        MoreVH(View v) {
            super(v);
            tvMore = v.findViewById(R.id.tv_more_reply);
        }
    }

    public interface OnCommentActionListener {
        void onReply(CommentItem item);
        void onToggleReplies(String parentId, boolean expand);
        void onLike(CommentItem item);
    }
}
