package com.noworld.notemap.ui.chat;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.noworld.notemap.R;
import com.noworld.notemap.data.ChatRepository;
import com.noworld.notemap.data.dto.ConversationResponse;

import java.util.List;

public class ChatListActivity extends AppCompatActivity {

    private ChatRepository repository;
    private ChatConversationAdapter adapter;
    private SwipeRefreshLayout refreshLayout;
    private ProgressBar progressBar;
    private TextView emptyView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_list);

        Toolbar toolbar = findViewById(R.id.toolbar_chat_list);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("私聊");
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        repository = new ChatRepository(this);

        RecyclerView rv = findViewById(R.id.rv_conversations);
        refreshLayout = findViewById(R.id.swipe_refresh);
        progressBar = findViewById(R.id.progress);
        emptyView = findViewById(R.id.tv_empty);

        adapter = new ChatConversationAdapter(this::openChat);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        refreshLayout.setOnRefreshListener(this::loadData);

        loadData();
    }

    private void loadData() {
        progressBar.setVisibility(View.VISIBLE);
        repository.fetchConversations(new ChatRepository.DataCallback<List<ConversationResponse>>() {
            @Override
            public void onSuccess(List<ConversationResponse> data) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    refreshLayout.setRefreshing(false);
                    adapter.setData(data);
                    emptyView.setVisibility(data == null || data.isEmpty() ? View.VISIBLE : View.GONE);
                });
            }

            @Override
            public void onError(Throwable throwable) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    refreshLayout.setRefreshing(false);
                    emptyView.setVisibility(View.VISIBLE);
                });
            }
        });
    }

    private void openChat(ConversationResponse conversation) {
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra(ChatActivity.EXTRA_PEER_ID, conversation.peerId);
        intent.putExtra(ChatActivity.EXTRA_PEER_NAME, conversation.peerName);
        intent.putExtra(ChatActivity.EXTRA_PEER_AVATAR, conversation.peerAvatar);
        startActivity(intent);
    }
}
