package com.noworld.notemap.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.noworld.notemap.R;

/**
 * Splash screen.
 */
public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Navigate after a 2000 ms (2s) delay.
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            // 1. Launch the main screen.
            // Note: this could be LoginActivity if you want users to log in first.
            // Typical flow: Splash -> check login -> (MainActivity or LoginActivity).
            // For simplicity, we go to MainActivity and let it handle login checks.
            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
            startActivity(intent);

            // 2. Close this screen so Back does not return to splash.
            finish();

            // Add a fade transition for smoother UX.
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }, 2000);
    }
}
