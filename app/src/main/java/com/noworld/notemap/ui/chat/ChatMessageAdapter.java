package com.noworld.notemap.ui.chat;

import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.noworld.notemap.R;
import com.noworld.notemap.data.dto.ChatMessageResponse;
import com.noworld.notemap.data.UserStore;
import com.noworld.notemap.ui.PictureActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatMessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_OUT = 1;
    private static final int TYPE_IN = 2;

    private final List<ChatMessageResponse> data = new ArrayList<>();
    private final String selfUid;
    private final SimpleDateFormat fmt = new SimpleDateFormat("HH:mm", Locale.getDefault());

    public ChatMessageAdapter(UserStore store) {
        this.selfUid = store != null ? store.getUid() : null;
    }

    public void setMessages(List<ChatMessageResponse> messages) {
        data.clear();
        if (messages != null) {
            data.addAll(messages);
        }
        notifyDataSetChanged();
    }

    public void addMessage(ChatMessageResponse message) {
        data.add(message);
        notifyItemInserted(data.size() - 1);
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessageResponse m = data.get(position);
        if (selfUid != null && selfUid.equals(m.fromUserId)) {
            return TYPE_OUT;
        }
        return TYPE_IN;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout = viewType == TYPE_OUT ? R.layout.item_chat_message_outgoing : R.layout.item_chat_message_incoming;
        View v = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
        return new MessageHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        MessageHolder h = (MessageHolder) holder;
        ChatMessageResponse msg = data.get(position);
        boolean isImage = "image".equals(msg.mediaType);
        boolean isVideo = "video".equals(msg.mediaType);
        h.time.setText(msg.createdAt > 0 ? fmt.format(new Date(msg.createdAt)) : "");

        if (isImage) {
            h.image.setVisibility(View.VISIBLE);
            h.video.setVisibility(View.GONE);
            h.text.setVisibility(TextUtils.isEmpty(msg.content) ? View.GONE : View.VISIBLE);
            h.text.setText(msg.content);
            Glide.with(h.image.getContext())
                    .load(resolvePath(msg.mediaUrl))
                    .placeholder(R.drawable.ic_picture)
                    .into(h.image);
            h.image.setOnClickListener(v -> openImage(v, msg.mediaUrl));
        } else if (isVideo) {
            h.image.setVisibility(View.GONE);
            h.video.setVisibility(View.VISIBLE);
            h.text.setVisibility(TextUtils.isEmpty(msg.content) ? View.GONE : View.VISIBLE);
            h.text.setText(!TextUtils.isEmpty(msg.content) ? msg.content : "Video");
            h.video.setOnClickListener(v -> openExternalPlayer(v, msg.mediaUrl));
        } else {
            h.image.setVisibility(View.GONE);
            h.video.setVisibility(View.GONE);
            h.text.setVisibility(View.VISIBLE);
            h.text.setText(!TextUtils.isEmpty(msg.content) ? msg.content : "");
        }
    }

    private void openExternalPlayer(View v, String url) {
        if (TextUtils.isEmpty(url)) return;
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(toUri(url), "video/*");
            v.getContext().startActivity(intent);
        } catch (Exception ignored) {
        }
    }

    private void openImage(View v, String url) {
        if (TextUtils.isEmpty(url)) return;
        try {
            Intent intent = new Intent(v.getContext(), PictureActivity.class);
            intent.putExtra(PictureActivity.EXTRA_IMAGE_URL, url);
            v.getContext().startActivity(intent);
        } catch (Exception ignored) {
        }
    }

    private Object resolvePath(String path) {
        if (TextUtils.isEmpty(path)) return null;
        if (path.startsWith("http") || path.startsWith("file:") || path.startsWith("content:")) {
            return path;
        }
        return new java.io.File(path);
    }

    private Uri toUri(String path) {
        if (TextUtils.isEmpty(path)) return null;
        if (path.startsWith("file:") || path.startsWith("content:") || path.startsWith("http")) {
            return Uri.parse(path);
        }
        return Uri.fromFile(new java.io.File(path));
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class MessageHolder extends RecyclerView.ViewHolder {
        TextView text;
        TextView time;
        ImageView image;
        View video;

        MessageHolder(@NonNull View itemView) {
            super(itemView);
            text = itemView.findViewById(R.id.tv_message);
            time = itemView.findViewById(R.id.tv_time);
            image = itemView.findViewById(R.id.iv_image);
            video = itemView.findViewById(R.id.layout_video);
        }
    }
}
