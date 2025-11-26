package com.noworld.notemap.data;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.noworld.notemap.data.dto.LoginRequest;
import com.noworld.notemap.data.dto.LoginResponse;
import com.noworld.notemap.data.dto.RegisterRequest;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AuthRepository {

    public interface LoginCallback {
        void onSuccess(LoginResponse.UserDto user);

        void onError(@NonNull Throwable throwable);
    }

    private final ApiService apiService;
    private final TokenStore tokenStore;
    private final UserStore userStore;

    public AuthRepository(Context context) {
        apiService = ApiClient.getService(context);
        tokenStore = TokenStore.getInstance(context);
        userStore = UserStore.getInstance(context);
    }

    public void login(String email, String password, LoginCallback callback) {
        apiService.login(new LoginRequest(email, password)).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(@NonNull Call<LoginResponse> call, @NonNull Response<LoginResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse body = response.body();
                    tokenStore.saveToken(body.getToken());
                    LoginResponse.UserDto user = body.getUser();
                    if (user == null) {
                        user = new LoginResponse.UserDto();
                    }
                    String tokenUid = userStore.extractUidFromToken(body.getToken());
                    if (TextUtils.isEmpty(user.uid)) {
                        user.uid = tokenUid;
                    }
                    LoginResponse.UserDto savedUser = userStore.saveUser(user);
                    callback.onSuccess(savedUser);
                } else {
                    callback.onError(new IllegalStateException("登录失败"));
                }
            }

            @Override
            public void onFailure(@NonNull Call<LoginResponse> call, @NonNull Throwable t) {
                callback.onError(t);
            }
        });
    }

    public void register(String email, String password, String nickname, LoginCallback callback) {
        apiService.register(new RegisterRequest(email, password, nickname)).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(@NonNull Call<LoginResponse> call, @NonNull Response<LoginResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse body = response.body();
                    tokenStore.saveToken(body.getToken());
                    LoginResponse.UserDto user = body.getUser();
                    if (user == null) {
                        user = new LoginResponse.UserDto();
                    }
                    if (TextUtils.isEmpty(user.username)) {
                        user.username = nickname;
                    }
                    String tokenUid = userStore.extractUidFromToken(body.getToken());
                    if (TextUtils.isEmpty(user.uid)) {
                        user.uid = tokenUid;
                    }
                    LoginResponse.UserDto savedUser = userStore.saveUser(user);
                    callback.onSuccess(savedUser);
                } else {
                    callback.onError(new IllegalStateException("注册失败"));
                }
            }

            @Override
            public void onFailure(@NonNull Call<LoginResponse> call, @NonNull Throwable t) {
                callback.onError(t);
            }
        });
    }
}
