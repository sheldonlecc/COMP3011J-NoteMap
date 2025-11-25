package com.noworld.notemap.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.noworld.notemap.R;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText etEmail;
    private TextInputEditText etNickname;
    private TextInputEditText etPassword;
    private TextInputEditText etConfirmPassword;
    private Button btnRegister;
    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    private boolean isLoading = false;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Runnable timeoutRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        initView();
        initEvent();
    }

    private void initView() {
        etEmail = findViewById(R.id.et_reg_email);
        etNickname = findViewById(R.id.et_reg_nickname);
        etPassword = findViewById(R.id.et_reg_password);
        etConfirmPassword = findViewById(R.id.et_reg_confirm_password);
        btnRegister = findViewById(R.id.btn_register);
        TextView tvBackToLogin = findViewById(R.id.tv_back_to_login);
        tvBackToLogin.setOnClickListener(v -> finish());
    }

    private void initEvent() {
        btnRegister.setOnClickListener(v -> attemptRegister());
    }

    private void attemptRegister() {
        if (isLoading) return;
        String email = getText(etEmail);
        String nickname = getText(etNickname);
        String password = getText(etPassword);
        String confirmPassword = getText(etConfirmPassword);

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("请输入邮箱");
            return;
        }
        if (TextUtils.isEmpty(nickname)) {
            etNickname.setError("请输入昵称");
            return;
        }
        if (TextUtils.isEmpty(password) || password.length() < 6) {
            etPassword.setError("密码至少6位");
            return;
        }
        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("两次输入的密码不一致");
            return;
        }

        setLoading(true);
        startTimeoutWatch();
        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    clearTimeoutWatch();
                    FirebaseUser user = authResult.getUser();
                    if (user == null) {
                        setLoading(false);
                        Toast.makeText(this, "注册失败，请重试", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    user.updateProfile(new UserProfileChangeRequest.Builder()
                                    .setDisplayName(nickname)
                                    .build())
                            .addOnSuccessListener(unused -> saveUserProfile(user, nickname, email))
                            .addOnFailureListener(e -> {
                                clearTimeoutWatch();
                                setLoading(false);
                                Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(e -> {
                    clearTimeoutWatch();
                    setLoading(false);
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void saveUserProfile(FirebaseUser user, String nickname, String email) {
        Map<String, Object> data = new HashMap<>();
        data.put("uid", user.getUid());
        data.put("username", nickname);
        data.put("email", email);
        data.put("avatarUrl", user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : null);
        data.put("signature", "这个人很神秘，还没写签名");
        data.put("liked_note_ids", new java.util.ArrayList<String>());

        firestore.collection("users")
                .document(user.getUid())
                .set(data, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    clearTimeoutWatch();
                    Toast.makeText(this, "注册成功", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    clearTimeoutWatch();
                    setLoading(false);
                    Toast.makeText(this, "保存用户信息失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private String getText(TextInputEditText editText) {
        return editText.getText() != null ? editText.getText().toString().trim() : "";
    }

    private void setLoading(boolean loading) {
        isLoading = loading;
        btnRegister.setEnabled(!loading);
        btnRegister.setText(loading ? "注册中..." : "立即注册");
    }

    private void startTimeoutWatch() {
        clearTimeoutWatch();
        timeoutRunnable = () -> {
            setLoading(false);
            Toast.makeText(this, "注册超时，请检查网络或稍后重试", Toast.LENGTH_LONG).show();
        };
        mainHandler.postDelayed(timeoutRunnable, 15000); // 15s 超时兜底
    }

    private void clearTimeoutWatch() {
        if (timeoutRunnable != null) {
            mainHandler.removeCallbacks(timeoutRunnable);
            timeoutRunnable = null;
        }
    }
}
