package com.noworld.notemap.data;

import android.content.Context;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.noworld.notemap.data.dto.ChatMessageResponse;
import com.noworld.notemap.data.dto.ConversationResponse;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 本地存储私聊数据（离线）：文件保存在内部存储，不走服务端。
 */
class LocalChatStore {
    private final File storeFile;
    private final Gson gson = new Gson();
    private final Object lock = new Object();

    private static class PeerMeta {
        String peerId;
        String peerName;
        String peerAvatar;
        long lastTimestamp;
        String lastPreview;
        int unreadCount;
    }

    private static class StoreData {
        Map<String, List<ChatMessageResponse>> messages = new HashMap<>();
        Map<String, PeerMeta> peers = new HashMap<>();
    }

    private StoreData cache;

    LocalChatStore(Context context) {
        this.storeFile = new File(context.getFilesDir(), "chat_store.json");
        load();
    }

    List<ConversationResponse> getConversations() {
        synchronized (lock) {
            List<ConversationResponse> list = new ArrayList<>();
            for (PeerMeta meta : cache.peers.values()) {
                ConversationResponse c = new ConversationResponse();
                c.peerId = meta.peerId;
                c.peerName = meta.peerName;
                c.peerAvatar = meta.peerAvatar;
                c.lastMessagePreview = meta.lastPreview;
                c.lastTimestamp = meta.lastTimestamp;
                c.unreadCount = meta.unreadCount;
                list.add(c);
            }
            return list;
        }
    }

    List<ChatMessageResponse> getMessages(String peerId) {
        synchronized (lock) {
            List<ChatMessageResponse> list = cache.messages.get(peerId);
            if (list == null) return new ArrayList<>();
            return new ArrayList<>(list);
        }
    }

    void appendMessage(String peerId, ChatMessageResponse message, String peerName, String peerAvatar, boolean incoming) {
        synchronized (lock) {
            List<ChatMessageResponse> list = cache.messages.get(peerId);
            if (list == null) {
                list = new ArrayList<>();
                cache.messages.put(peerId, list);
            }
            list.add(message);
            PeerMeta meta = cache.peers.get(peerId);
            if (meta == null) {
                meta = new PeerMeta();
                meta.peerId = peerId;
                cache.peers.put(peerId, meta);
            }
            if (!TextUtils.isEmpty(peerName)) meta.peerName = peerName;
            if (!TextUtils.isEmpty(peerAvatar)) meta.peerAvatar = peerAvatar;
            meta.lastTimestamp = message.createdAt;
            meta.lastPreview = buildPreview(message);
            if (incoming) {
                meta.unreadCount = meta.unreadCount + 1;
            } else {
                meta.unreadCount = 0;
            }
            save();
        }
    }

    void updatePeer(String peerId, String peerName, String peerAvatar) {
        synchronized (lock) {
            PeerMeta meta = cache.peers.get(peerId);
            if (meta == null) {
                meta = new PeerMeta();
                meta.peerId = peerId;
                cache.peers.put(peerId, meta);
            }
            if (!TextUtils.isEmpty(peerName)) meta.peerName = peerName;
            if (!TextUtils.isEmpty(peerAvatar)) meta.peerAvatar = peerAvatar;
            save();
        }
    }

    void markRead(String peerId) {
        synchronized (lock) {
            PeerMeta meta = cache.peers.get(peerId);
            if (meta != null) {
                meta.unreadCount = 0;
                save();
            }
        }
    }

    private String buildPreview(ChatMessageResponse m) {
        if ("image".equals(m.mediaType)) return "[图片]";
        if ("video".equals(m.mediaType)) return "[视频]";
        return !TextUtils.isEmpty(m.content) ? m.content : "";
    }

    private void load() {
        synchronized (lock) {
            if (cache != null) return;
            if (!storeFile.exists()) {
                cache = new StoreData();
                return;
            }
            try (FileReader reader = new FileReader(storeFile)) {
                Type type = new TypeToken<StoreData>() {}.getType();
                cache = gson.fromJson(reader, type);
                if (cache == null) {
                    cache = new StoreData();
                }
            } catch (Exception e) {
                cache = new StoreData();
            }
        }
    }

    private void save() {
        synchronized (lock) {
            try (FileWriter writer = new FileWriter(storeFile, false)) {
                gson.toJson(cache, writer);
            } catch (Exception ignored) {
            }
        }
    }
}
