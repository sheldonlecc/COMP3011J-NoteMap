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
import java.util.List;

// 简单的评论数据类
class CommentItem {
    String userName;
    String content;
    String time;
    // 如果之后有头像URL，可以在这里加一个 String avatarUrl;
    public CommentItem(String u, String c, String t) { userName = u; content = c; time = t; }
}

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.VH> {

    private List<CommentItem> data;

    public CommentAdapter(List<CommentItem> data) {
        this.data = data;
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
        holder.tvUser.setText(item.userName);
        holder.tvContent.setText(item.content);
        holder.tvTime.setText(item.time);

        // 3. 【核心修改】使用 Glide 加载头像并裁剪为圆形
        Glide.with(holder.itemView.getContext())
                .load(R.drawable.ic_profile) // 这里暂时加载默认图，如果您以后有了 URL，换成 item.avatarUrl
                .placeholder(R.drawable.ic_profile) // 加载中显示的图
                .error(R.drawable.ic_profile)       // 错误时显示的图
                .circleCrop()                       // 【重点】强制变成圆形
                .into(holder.ivAvatar);
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
}