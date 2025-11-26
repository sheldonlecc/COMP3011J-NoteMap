package com.noworld.notemap.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.noworld.notemap.R;
import com.noworld.notemap.data.AuthRepository;

public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText etEmail;
    private TextInputEditText etNickname;
    private TextInputEditText etPassword;
    private TextInputEditText etConfirmPassword;
    private Button btnRegister;
    private AuthRepository authRepository;
    private boolean isLoading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        authRepository = new AuthRepository(this);
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
        authRepository.register(email, password, nickname, new AuthRepository.LoginCallback() {
            @Override
            public void onSuccess(com.noworld.notemap.data.dto.LoginResponse.UserDto user) {
                Toast.makeText(RegisterActivity.this, "注册成功", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onError(@NonNull Throwable throwable) {
                setLoading(false);
                Toast.makeText(RegisterActivity.this, "注册失败: " + throwable.getMessage(), Toast.LENGTH_LONG).show();
            }
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
}
