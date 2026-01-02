package com.noworld.notemap.ui.chat;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.noworld.notemap.R;
import com.noworld.notemap.data.ChatRepository;
import com.noworld.notemap.data.UserStore;
import com.noworld.notemap.data.dto.ChatMessageResponse;

import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity {

    public static final String EXTRA_PEER_ID = "EXTRA_PEER_ID";
    public static final String EXTRA_PEER_NAME = "EXTRA_PEER_NAME";
    public static final String EXTRA_PEER_AVATAR = "EXTRA_PEER_AVATAR";

    private ChatRepository repository;
    private ChatMessageAdapter adapter;
    private final List<ChatMessageResponse> messages = new ArrayList<>();
    private RecyclerView rvMessages;
    private ProgressBar progressBar;
    private EditText etInput;
    private ImageButton btnSend;
    private ImageButton btnImage;
    private ActivityResultLauncher<Intent> pickMediaLauncher;
    private ActivityResultLauncher<String> permissionLauncher;
    private String peerId;
    private String peerName;
    private String peerAvatar;
    private String pendingType = "image";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        peerId = getIntent().getStringExtra(EXTRA_PEER_ID);
        peerName = getIntent().getStringExtra(EXTRA_PEER_NAME);
        peerAvatar = getIntent().getStringExtra(EXTRA_PEER_AVATAR);
        if (TextUtils.isEmpty(peerId)) {
            Toast.makeText(this, "Missing chat target", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        repository = new ChatRepository(this);
        adapter = new ChatMessageAdapter(UserStore.getInstance(this));

        Toolbar toolbar = findViewById(R.id.toolbar_chat);
        ImageView ivAvatar = findViewById(R.id.iv_peer_avatar);
        TextView tvPeerName = findViewById(R.id.tv_peer_name);
        toolbar.setTitle("");
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
        tvPeerName.setText(TextUtils.isEmpty(peerName) ? "Direct message" : peerName);
        Glide.with(this).load(peerAvatar).placeholder(R.drawable.ic_profile).circleCrop().into(ivAvatar);

        rvMessages = findViewById(R.id.rv_messages);
        progressBar = findViewById(R.id.progress);
        etInput = findViewById(R.id.et_message);
        btnSend = findViewById(R.id.btn_send);
        btnImage = findViewById(R.id.btn_image);

        rvMessages.setLayoutManager(new LinearLayoutManager(this));
        rvMessages.setAdapter(adapter);

        btnSend.setOnClickListener(v -> sendText());
        btnImage.setOnClickListener(v -> requestPick("image"));

        initPickers();
        loadMessages();
    }

    private void initPickers() {
        pickMediaLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                Uri uri = result.getData().getData();
                if (uri != null) {
                    sendMedia(uri, pendingType);
                }
            }
        });
        permissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
            if (granted) {
                openPicker(pendingType);
            } else {
                Toast.makeText(this, "Storage permission required to pick media", Toast.LENGTH_SHORT).show();
            }
        });

        // Long press to pick video
        btnImage.setOnLongClickListener(v -> {
            requestPick("video");
            return true;
        });
    }

    private void requestPick(String type) {
        pendingType = type;
        String permission;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permission = "image".equals(type) ? Manifest.permission.READ_MEDIA_IMAGES : Manifest.permission.READ_MEDIA_VIDEO;
        } else {
            permission = Manifest.permission.READ_EXTERNAL_STORAGE;
        }
        if (ContextCompat.checkSelfPermission(this, permission) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            openPicker(type);
        } else {
            permissionLauncher.launch(permission);
        }
    }

    private void openPicker(String type) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image".equals(type) ? "image/*" : "video/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        pickMediaLauncher.launch(intent);
    }

    private void loadMessages() {
        progressBar.setVisibility(View.VISIBLE);
        repository.fetchMessages(peerId, null, new ChatRepository.DataCallback<List<ChatMessageResponse>>() {
            @Override
            public void onSuccess(List<ChatMessageResponse> data) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    messages.clear();
                    if (data != null) {
                        messages.addAll(data);
                    }
                    adapter.setMessages(messages);
                    scrollToBottom();
                    repository.markRead(peerId);
                });
            }

            @Override
            public void onError(Throwable throwable) {
                runOnUiThread(() -> progressBar.setVisibility(View.GONE));
            }
        });
    }

    private void sendText() {
        String text = etInput.getText().toString().trim();
        if (TextUtils.isEmpty(text)) return;
        etInput.setText("");
        repository.sendTextMessage(peerId, text, new ChatRepository.DataCallback<ChatMessageResponse>() {
            @Override
            public void onSuccess(ChatMessageResponse data) {
                runOnUiThread(() -> {
                    adapter.addMessage(data);
                    scrollToBottom();
                });
            }

            @Override
            public void onError(Throwable throwable) {
                runOnUiThread(() -> Toast.makeText(ChatActivity.this, "Send failed", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void sendMedia(Uri uri, String mediaType) {
        progressBar.setVisibility(View.VISIBLE);
        repository.sendMediaMessage(peerId, uri, mediaType, new ChatRepository.DataCallback<ChatMessageResponse>() {
            @Override
            public void onSuccess(ChatMessageResponse data) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    adapter.addMessage(data);
                    scrollToBottom();
                });
            }

            @Override
            public void onError(Throwable throwable) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(ChatActivity.this, "Send failed", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void scrollToBottom() {
        rvMessages.scrollToPosition(Math.max(adapter.getItemCount() - 1, 0));
    }
}
