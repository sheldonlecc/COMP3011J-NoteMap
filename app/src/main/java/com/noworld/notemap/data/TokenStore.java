package com.noworld.notemap.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

/**
 * 简单的 token 持久化。
 */
public class TokenStore {
    private static final String PREF_NAME = "aliyun_auth_pref";
    private static final String KEY_TOKEN = "auth_token";
    private static TokenStore instance;
    private final SharedPreferences prefs;

    private TokenStore(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized TokenStore getInstance(Context context) {
        if (instance == null) {
            instance = new TokenStore(context);
        }
        return instance;
    }

    public void saveToken(String token) {
        prefs.edit().putString(KEY_TOKEN, token).apply();
    }

    public String getToken() {
        String token = prefs.getString(KEY_TOKEN, null);
        return TextUtils.isEmpty(token) ? null : token;
    }

    public void clear() {
        prefs.edit().remove(KEY_TOKEN).apply();
    }
}
