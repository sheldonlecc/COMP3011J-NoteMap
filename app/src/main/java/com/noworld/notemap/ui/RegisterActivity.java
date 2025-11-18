package com.noworld.notemap.ui;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.noworld.notemap.R;

public class RegisterActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        Button btnRegister = findViewById(R.id.btn_register);

        // 点击注册
        btnRegister.setOnClickListener(v -> {
            // TODO: Member C - 接入 Firebase 注册逻辑
            Toast.makeText(RegisterActivity.this, "UI演示：注册请求已发送", Toast.LENGTH_SHORT).show();
            // 注册成功后通常直接结束当前页，返回登录页
            finish();
        });

        // 点击"返回登录"
        findViewById(R.id.tv_back_to_login).setOnClickListener(v -> finish());
    }
}