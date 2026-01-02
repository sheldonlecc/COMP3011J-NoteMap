package com.noworld.notemap.data;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;

import com.noworld.notemap.data.dto.ChatMessageResponse;
import com.noworld.notemap.data.dto.ConversationResponse;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Local chat data layer: wraps conversations, messages, and sending text/media.
 */
public class ChatRepository {

    public interface DataCallback<T> {
        void onSuccess(T data);

        void onError(Throwable throwable);
    }

    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final LocalChatStore chatStore;
    private final LocalMediaStore mediaStore;
    private final UserStore userStore;

    public ChatRepository(Context context) {
        Context appCtx = context.getApplicationContext();
        this.chatStore = new LocalChatStore(appCtx);
        this.mediaStore = new LocalMediaStore(appCtx);
        this.userStore = UserStore.getInstance(appCtx);
    }

    public void fetchConversations(DataCallback<List<ConversationResponse>> callback) {
        io.execute(() -> callback.onSuccess(chatStore.getConversations()));
    }

    public void fetchMessages(String peerId, String sinceId, DataCallback<List<ChatMessageResponse>> callback) {
        io.execute(() -> {
            List<ChatMessageResponse> list = chatStore.getMessages(peerId);
            chatStore.markRead(peerId);
            callback.onSuccess(list);
        });
    }

    public void ensurePeerMeta(String peerId, String peerName, String peerAvatar) {
        io.execute(() -> chatStore.updatePeer(peerId, peerName, peerAvatar));
    }

    public void sendTextMessage(String peerId, String text, DataCallback<ChatMessageResponse> callback) {
        io.execute(() -> {
            ChatMessageResponse message = buildMessage(peerId, text, null, "text");
            chatStore.appendMessage(peerId, message, null, null, false);
            callback.onSuccess(message);
        });
    }

    public Future<?> sendMediaMessage(String peerId, Uri uri, String mediaType, DataCallback<ChatMessageResponse> callback) {
        return io.submit(() -> {
            try {
                String localPath = mediaStore.copyToLocal(uri, mediaType);
                if (TextUtils.isEmpty(localPath)) {
                    callback.onError(new IllegalStateException("Failed to save media locally"));
                    return;
                }
                ChatMessageResponse message = buildMessage(peerId, null, localPath, mediaType);
                chatStore.appendMessage(peerId, message, null, null, false);
                callback.onSuccess(message);
            } catch (Exception e) {
                callback.onError(e);
            }
        });
    }

    public void markRead(String peerId) {
        io.execute(() -> chatStore.markRead(peerId));
    }

    private ChatMessageResponse buildMessage(String peerId, String content, String mediaUrl, String mediaType) {
        ChatMessageResponse message = new ChatMessageResponse();
        message.id = UUID.randomUUID().toString();
        message.fromUserId = userStore.getUid();
        message.toUserId = peerId;
        message.content = content;
        message.mediaUrl = mediaUrl;
        message.mediaType = mediaType;
        message.createdAt = System.currentTimeMillis();
        return message;
    }
}
