package com.noworld.notemap.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import java.util.HashSet;
import java.util.Set;

/**
 * 持久化当前用户点赞过的笔记 ID，解决重新进入后点赞状态丢失问题。
 */
public class LikedStore {

    private static final String PREF_NAME = "aliyun_like_pref";
    private static final String KEY_LIKED_PREFIX = "liked_"; // 后缀为 uid
    private static final String KEY_COUNT = "like_counts"; // 全局存储 id:count

    private static LikedStore instance;
    private final SharedPreferences prefs;

    private LikedStore(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized LikedStore getInstance(Context context) {
        if (instance == null) {
            instance = new LikedStore(context);
        }
        return instance;
    }

    public Set<String> getLikedIds(String uid) {
        if (TextUtils.isEmpty(uid)) return new HashSet<>();
        return new HashSet<>(prefs.getStringSet(KEY_LIKED_PREFIX + uid, new HashSet<>()));
    }

    public void setLikedIds(String uid, Set<String> ids) {
        if (TextUtils.isEmpty(uid)) return;
        prefs.edit().putStringSet(KEY_LIKED_PREFIX + uid, ids != null ? ids : new HashSet<>()).apply();
    }

    public void toggle(String uid, String noteId, boolean liked) {
        if (TextUtils.isEmpty(uid) || TextUtils.isEmpty(noteId)) return;
        Set<String> likedSet = getLikedIds(uid);
        if (liked) {
            likedSet.add(noteId);
        } else {
            likedSet.remove(noteId);
        }
        setLikedIds(uid, likedSet);
    }

    public void saveLikeCount(String noteId, int count) {
        if (TextUtils.isEmpty(noteId)) return;
        Set<String> raw = new HashSet<>(prefs.getStringSet(KEY_COUNT, new HashSet<>()));
        raw.removeIf(s -> s != null && s.startsWith(noteId + ":"));
        raw.add(noteId + ":" + Math.max(0, count));
        prefs.edit().putStringSet(KEY_COUNT, raw).apply();
    }

    public Integer getLikeCount(String noteId) {
        if (TextUtils.isEmpty(noteId)) return null;
        Set<String> raw = prefs.getStringSet(KEY_COUNT, new HashSet<>());
        for (String s : raw) {
            if (s != null && s.startsWith(noteId + ":")) {
                try {
                    return Integer.parseInt(s.substring(noteId.length() + 1));
                } catch (Exception ignored) { }
            }
        }
        return null;
    }
}
