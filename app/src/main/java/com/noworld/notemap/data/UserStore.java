package com.noworld.notemap.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.noworld.notemap.data.dto.LoginResponse;

import org.json.JSONObject;

import java.util.UUID;

/**
 * 本地存储登录用户信息（id、昵称、头像）。
 */
public class UserStore {
    private static final String PREF_NAME = "aliyun_user_pref";
    private static final String KEY_UID = "uid";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_AVATAR = "avatar";
    private static final String KEY_AVATAR_PREFIX = "avatar_uid_";

    private static UserStore instance;
    private final SharedPreferences prefs;

    // 1. 定义一个 Key 用来存背景图路径
    private static final String KEY_PROFILE_BG = "profile_bg_uri";

    private UserStore(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized UserStore getInstance(Context context) {
        if (instance == null) {
            instance = new UserStore(context);
        }
        return instance;
    }

    /**
     * 保存用户信息并自动为缺失字段填充默认值/唯一 ID。
     */
    public LoginResponse.UserDto saveUser(@Nullable LoginResponse.UserDto user) {
        LoginResponse.UserDto existing = getUser();
        String finalUid = ensureUid(user != null ? user.uid : null,
                existing != null ? existing.uid : null);
        String finalUsername = !TextUtils.isEmpty(user != null ? user.username : null)
                ? user.username
                : (existing != null && !TextUtils.isEmpty(existing.username)
                ? existing.username
                : "Map user");
        String finalAvatar = !TextUtils.isEmpty(user != null ? user.avatarUrl : null)
                ? user.avatarUrl
                : (existing != null ? existing.avatarUrl : null);

        prefs.edit()
                .putString(KEY_UID, finalUid)
                .putString(KEY_USERNAME, finalUsername)
                .putString(KEY_AVATAR, finalAvatar)
                .apply();
        if (!TextUtils.isEmpty(finalUid) && !TextUtils.isEmpty(finalAvatar)) {
            prefs.edit().putString(KEY_AVATAR_PREFIX + finalUid, finalAvatar).apply();
        }

        LoginResponse.UserDto saved = new LoginResponse.UserDto();
        saved.uid = finalUid;
        saved.username = finalUsername;
        saved.avatarUrl = finalAvatar;
        return saved;
    }

    public String getUid() {
        String uid = prefs.getString(KEY_UID, null);
        return TextUtils.isEmpty(uid) ? null : uid;
    }

    public String getUsername() {
        return prefs.getString(KEY_USERNAME, null);
    }

    public String getAvatarUrl() {
        String current = prefs.getString(KEY_AVATAR, null);
        if (!TextUtils.isEmpty(current)) return current;
        String uid = getUid();
        if (!TextUtils.isEmpty(uid)) {
            return prefs.getString(KEY_AVATAR_PREFIX + uid, null);
        }
        return null;
    }

    public LoginResponse.UserDto getUser() {
        String uid = prefs.getString(KEY_UID, null);
        String username = prefs.getString(KEY_USERNAME, null);
        String avatar = prefs.getString(KEY_AVATAR, null);
        if (TextUtils.isEmpty(uid) && TextUtils.isEmpty(username) && TextUtils.isEmpty(avatar)) {
            return null;
        }
        LoginResponse.UserDto user = new LoginResponse.UserDto();
        user.uid = uid;
        user.username = username;
        if (!TextUtils.isEmpty(avatar)) {
            user.avatarUrl = avatar;
        } else if (!TextUtils.isEmpty(uid)) {
            user.avatarUrl = prefs.getString(KEY_AVATAR_PREFIX + uid, null);
        }
        return user;
    }

    public void updateAvatar(String avatarUrl) {
        if (TextUtils.isEmpty(avatarUrl)) return;
        String uid = getUid();
        prefs.edit().putString(KEY_AVATAR, avatarUrl).apply();
        if (!TextUtils.isEmpty(uid)) {
            prefs.edit().putString(KEY_AVATAR_PREFIX + uid, avatarUrl).apply();
        }
    }

    public void updateUsername(String username) {
        if (TextUtils.isEmpty(username)) return;
        prefs.edit().putString(KEY_USERNAME, username).apply();
    }

    public String ensureUid(@Nullable String... candidateUids) {
        String uid = null;
        if (candidateUids != null) {
            for (String c : candidateUids) {
                if (!TextUtils.isEmpty(c)) {
                    uid = c;
                    break;
                }
            }
        }
        if (TextUtils.isEmpty(uid)) {
            uid = generateUid();
        }
        prefs.edit().putString(KEY_UID, uid).apply();
        return uid;
    }

    private String generateUid() {
        String raw = UUID.randomUUID().toString().replace("-", "");
        return "NM-" + raw.substring(0, 10).toUpperCase();
    }

    public void clear() {
        prefs.edit()
                .remove(KEY_UID)
                .remove(KEY_USERNAME)
                .remove(KEY_AVATAR)
                .apply(); // 保留按 uid 存储的头像，用于重新登录后回显
    }

    /**
     * 从 JWT token 的 payload 中提取 uid 字段，用于与后端保持一致。
     */
    public String extractUidFromToken(@Nullable String token) {
        if (TextUtils.isEmpty(token) || !token.contains(".")) return null;
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) return null;
            String payload = parts[1];
            byte[] decoded = android.util.Base64.decode(payload, android.util.Base64.URL_SAFE | android.util.Base64.NO_WRAP);
            JSONObject json = new JSONObject(new String(decoded));
            if (json.has("uid")) {
                Object uidVal = json.get("uid");
                return String.valueOf(uidVal);
            }
        } catch (Exception e) {
            // ignore, fallback to server提供的 uid
        }
        return null;
    }

    /**
     * 保存背景图的 URI 字符串
     */
    public void setProfileBg(String uriString) {
        prefs.edit().putString(KEY_PROFILE_BG, uriString).apply();
    }

    /**
     * 获取背景图的 URI 字符串
     */
    public String getProfileBg() {
        return prefs.getString(KEY_PROFILE_BG, null);
    }
}
