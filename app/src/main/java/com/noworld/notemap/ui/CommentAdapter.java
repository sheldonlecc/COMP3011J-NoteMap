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

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.VH> {

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
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_comment, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        CommentItem item = data.get(position);
        holder.tvUser.setText(item.getUserName());
        // 显示“回复 xxx：内容”
        if (item.getReplyToUserName() != null && !item.getReplyToUserName().isEmpty()) {
            holder.tvContent.setText("回复 " + item.getReplyToUserName() + "：" + item.getContent());
        } else {
            holder.tvContent.setText(item.getContent());
        }
        holder.tvTime.setText(item.getTime());

        // 二级评论缩进
        ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) holder.itemView.getLayoutParams();
        if (lp != null) {
            int indent = 0;
            if (item.isReply()) {
                float density = holder.itemView.getResources().getDisplayMetrics().density;
                indent = (int) (16 * density);
            }
            lp.leftMargin = indent;
            holder.itemView.setLayoutParams(lp);
        }

        // 3. 【核心修改】使用 Glide 加载头像并裁剪为圆形
        Glide.with(holder.itemView.getContext())
                .load(item.getAvatarUrl() != null ? item.getAvatarUrl() : R.drawable.ic_profile)
                .placeholder(R.drawable.ic_profile) // 加载中显示的图
                .error(R.drawable.ic_profile)       // 错误时显示的图
                .circleCrop()                       // 【重点】强制变成圆形
                .into(holder.ivAvatar);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onReply(item);
            }
        });
    }

    @Override
    public int getItemCount() { return data.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvUser, tvContent, tvTime;
        ImageView ivAvatar; // 4. 声明 ImageView

        VH(View v) {
            super(v);
            tvUser = v.findViewById(R.id.tv_comment_user);
            tvContent = v.findViewById(R.id.tv_comment_content);
            tvTime = v.findViewById(R.id.tv_comment_time);

            // 5. 绑定 XML 里的头像 ID (通常是 iv_comment_avatar)
            ivAvatar = v.findViewById(R.id.iv_comment_avatar);
        }
    }

    public interface OnCommentActionListener {
        void onReply(CommentItem item);
    }
}
