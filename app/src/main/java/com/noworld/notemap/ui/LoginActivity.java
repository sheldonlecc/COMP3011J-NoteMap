package com.noworld.notemap.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.noworld.notemap.R;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvRegister;
    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    private boolean isLoading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        initView();
        initEvent();
    }

    private void initView() {
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        tvRegister = findViewById(R.id.tv_register);
    }

    private void initEvent() {
        btnLogin.setOnClickListener(v -> attemptLogin());
        tvRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    private void attemptLogin() {
        if (isLoading) return;
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("请输入邮箱");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("请输入密码");
            return;
        }

        setLoading(true);
        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> syncUserProfile(authResult.getUser()))
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(LoginActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void syncUserProfile(FirebaseUser user) {
        if (user == null) {
            setLoading(false);
            Toast.makeText(this, "用户信息获取失败，请重试", Toast.LENGTH_LONG).show();
            return;
        }
        Map<String, Object> profile = new HashMap<>();
        profile.put("uid", user.getUid());
        profile.put("email", user.getEmail());
        profile.put("username", !TextUtils.isEmpty(user.getDisplayName()) ? user.getDisplayName() : "地图用户");
        profile.put("avatarUrl", user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : null);
        firestore.collection("users")
                .document(user.getUid())
                .set(profile, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    Toast.makeText(LoginActivity.this, "登录成功", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(LoginActivity.this, "登录成功，但同步用户资料失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void setLoading(boolean loading) {
        isLoading = loading;
        btnLogin.setEnabled(!loading);
        btnLogin.setText(loading ? "登录中..." : "登 录");
    }
}
