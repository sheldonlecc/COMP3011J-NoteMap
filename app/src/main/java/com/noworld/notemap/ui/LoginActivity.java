package com.noworld.notemap.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import com.noworld.notemap.R;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

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
        // 登录按钮点击事件
        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString();
            String password = etPassword.getText().toString();

            // TODO: Member C - 在这里实现 Firebase 登录逻辑
            // 目前仅做简单的判空演示
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(LoginActivity.this, "请输入邮箱和密码", Toast.LENGTH_SHORT).show();
            } else {
                // 模拟登录成功
                Toast.makeText(LoginActivity.this, "UI测试：模拟登录成功", Toast.LENGTH_SHORT).show();
                finish(); // 关闭登录页，返回上一页
            }
        });

        // 注册点击事件
        tvRegister.setOnClickListener(v -> {
            // 跳转到注册页
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }
}