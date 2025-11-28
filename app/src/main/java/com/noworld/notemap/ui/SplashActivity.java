package com.noworld.notemap.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.noworld.notemap.R;

/**
 * 启动页 (Splash Screen)
 */
public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // 延迟 2000 毫秒 (2秒) 后跳转
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            // 1. 启动主页
            // 注意：这里原来可能是 LoginActivity，如果你想让用户每次都先看登录页，就改成 LoginActivity.class
            // 但通常逻辑是：Splash -> 判断是否登录 -> (MainActivity 或 LoginActivity)
            // 为了简单，我们先直接跳 MainActivity，因为 MainActivity 里你有判断登录的逻辑
            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
            startActivity(intent);

            // 2. 关闭当前页面 (这样用户按返回键不会回到启动页)
            finish();

            // 增加一个淡入淡出的转场动画，看起来更顺滑
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }, 2000);
    }
}