package com.noworld.notemap.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.noworld.notemap.R;
import java.util.List;

// 简单的评论数据类 (你可以之后单独建一个文件)
class CommentItem {
    String userName;
    String content;
    String time;
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
        // 这里可以使用 Glide 加载头像
    }

    @Override
    public int getItemCount() { return data.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvUser, tvContent, tvTime;
        VH(View v) {
            super(v);
            tvUser = v.findViewById(R.id.tv_comment_user);
            tvContent = v.findViewById(R.id.tv_comment_content);
            tvTime = v.findViewById(R.id.tv_comment_time);
        }
    }
}