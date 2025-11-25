package com.noworld.notemap.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.noworld.notemap.R;

/**
 * 个人主页 Activity
 */
public class ProfileActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private ImageView ivAvatar;
    private TextView tvUsername;
    private TextView tvAccountId;
    private TextView tvSignature;
    private Button btnProfileAction;

    private TabLayout tabLayout;
    private FrameLayout contentContainer;
    private TextView tvWorksListPlaceholder;
    private TextView tvLikesListPlaceholder;

    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    private FirebaseAuth.AuthStateListener authStateListener;
    private ListenerRegistration profileListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        toolbar = findViewById(R.id.toolbar_profile);
        ivAvatar = findViewById(R.id.iv_avatar);
        tvUsername = findViewById(R.id.tv_username);
        tvAccountId = findViewById(R.id.tv_account_id);
        tvSignature = findViewById(R.id.tv_signature);
        btnProfileAction = findViewById(R.id.btn_profile_action);
        tabLayout = findViewById(R.id.tab_layout);
        contentContainer = findViewById(R.id.content_container);
        tvWorksListPlaceholder = findViewById(R.id.tv_works_list_placeholder);
        tvLikesListPlaceholder = findViewById(R.id.tv_likes_list_placeholder);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    tvWorksListPlaceholder.setVisibility(View.VISIBLE);
                    tvLikesListPlaceholder.setVisibility(View.GONE);
                } else {
                    tvWorksListPlaceholder.setVisibility(View.GONE);
                    tvLikesListPlaceholder.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) { }

            @Override
            public void onTabReselected(TabLayout.Tab tab) { }
        });

        authStateListener = firebaseAuth -> updateProfileUI(firebaseAuth.getCurrentUser());
        ivAvatar.setOnClickListener(v -> handleAvatarClick());
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (authStateListener != null) {
            auth.addAuthStateListener(authStateListener);
        }
        updateProfileUI(auth.getCurrentUser());
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (authStateListener != null) {
            auth.removeAuthStateListener(authStateListener);
        }
        detachProfileListener();
    }

    private void updateProfileUI(FirebaseUser user) {
        if (user == null) {
            detachProfileListener();
            tvUsername.setText("未登录用户");
            tvAccountId.setText("点击头像登录");
            tvSignature.setText("登录后可同步发布和点赞记录");
            btnProfileAction.setText("去登录");
            btnProfileAction.setOnClickListener(v -> startLogin());
            Glide.with(this).load(R.drawable.ic_profile).into(ivAvatar);
            return;
        }

        String displayName = !TextUtils.isEmpty(user.getDisplayName()) ? user.getDisplayName() : "地图用户";
        tvUsername.setText(displayName);
        tvAccountId.setText("UID: " + user.getUid());
        btnProfileAction.setText("退出登录");
        btnProfileAction.setOnClickListener(v -> {
            auth.signOut();
            Toast.makeText(this, "已退出登录", Toast.LENGTH_SHORT).show();
        });
        tvSignature.setText("这个人很神秘，还没写签名");
        if (user.getPhotoUrl() != null) {
            Glide.with(this).load(user.getPhotoUrl()).placeholder(R.drawable.ic_profile).into(ivAvatar);
        } else {
            Glide.with(this).load(R.drawable.ic_profile).into(ivAvatar);
        }
        listenUserProfile(user.getUid());
    }

    private void listenUserProfile(String uid) {
        detachProfileListener();
        profileListener = firestore.collection("users").document(uid)
                .addSnapshotListener((snapshot, error) -> {
                    if (snapshot != null && snapshot.exists()) {
                        bindUserSnapshot(snapshot);
                    }
                });
    }

    private void bindUserSnapshot(DocumentSnapshot snapshot) {
        String signature = snapshot.getString("signature");
        if (!TextUtils.isEmpty(signature)) {
            tvSignature.setText(signature);
        }
        String avatarUrl = snapshot.getString("avatarUrl");
        if (!TextUtils.isEmpty(avatarUrl)) {
            Glide.with(this).load(avatarUrl).placeholder(R.drawable.ic_profile).into(ivAvatar);
        }
    }

    private void detachProfileListener() {
        if (profileListener != null) {
            profileListener.remove();
            profileListener = null;
        }
    }

    private void handleAvatarClick() {
        if (auth.getCurrentUser() == null) {
            startLogin();
        }
    }

    private void startLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}

