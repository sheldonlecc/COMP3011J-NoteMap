package com.noworld.notemap.ui.chat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.noworld.notemap.R;
import com.noworld.notemap.data.dto.ConversationResponse;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatConversationAdapter extends RecyclerView.Adapter<ChatConversationAdapter.Holder> {

    public interface OnItemClickListener {
        void onClick(ConversationResponse conversation);
    }

    private final List<ConversationResponse> data = new ArrayList<>();
    private final OnItemClickListener listener;
    private final SimpleDateFormat fmt = new SimpleDateFormat("MM-dd HH:mm", Locale.getDefault());

    public ChatConversationAdapter(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setData(List<ConversationResponse> conversations) {
        data.clear();
        if (conversations != null) {
            data.addAll(conversations);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_conversation, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        ConversationResponse item = data.get(position);
        holder.name.setText(item.peerName != null ? item.peerName : "User");
        holder.lastMessage.setText(item.lastMessagePreview != null ? item.lastMessagePreview : "");
        if (item.lastTimestamp > 0) {
            holder.time.setText(fmt.format(new Date(item.lastTimestamp)));
        } else {
            holder.time.setText("");
        }
        if (item.unreadCount > 0) {
            holder.unread.setVisibility(View.VISIBLE);
            holder.unread.setText(String.valueOf(item.unreadCount));
        } else {
            holder.unread.setVisibility(View.GONE);
        }
        Glide.with(holder.avatar.getContext())
                .load(item.peerAvatar)
                .placeholder(R.drawable.ic_profile)
                .circleCrop()
                .into(holder.avatar);
        holder.itemView.setOnClickListener(v -> listener.onClick(item));
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        ImageView avatar;
        TextView name;
        TextView lastMessage;
        TextView time;
        TextView unread;

        Holder(@NonNull View itemView) {
            super(itemView);
            avatar = itemView.findViewById(R.id.iv_avatar);
            name = itemView.findViewById(R.id.tv_name);
            lastMessage = itemView.findViewById(R.id.tv_last_message);
            time = itemView.findViewById(R.id.tv_time);
            unread = itemView.findViewById(R.id.tv_unread);
        }
    }
}
