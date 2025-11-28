package com.noworld.notemap.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;

import com.noworld.notemap.R;
import com.noworld.notemap.data.AliNoteRepository;
import com.noworld.notemap.data.ApiClient;
import com.noworld.notemap.data.ApiService;
import com.noworld.notemap.data.dto.NotificationResponse;
import com.noworld.notemap.data.model.CommentItem;
import com.amap.apis.cluster.demo.RegionItem;

import java.util.ArrayList;
import java.util.List;

public class NotificationActivity extends AppCompatActivity {

    private AliNoteRepository noteRepository;
    private NotificationAdapter adapter;
    private ApiService api;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        Toolbar toolbar = findViewById(R.id.toolbar_notification);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        RecyclerView rv = findViewById(R.id.rv_notifications);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NotificationAdapter(new ArrayList<>(), this::handleNotificationClick);
        rv.setAdapter(adapter);

        noteRepository = AliNoteRepository.getInstance(this);
        api = ApiClient.getService(this);

        loadNotifications();
    }

    private void handleNotificationClick(NotificationItem item) {
        if (item == null || item.targetType == null) {
            return;
        }
        if ("note".equalsIgnoreCase(item.targetType)) {
            openNoteById(item.targetId, null);
        } else if ("comment".equalsIgnoreCase(item.targetType)) {
            if (!TextUtils.isEmpty(item.noteId)) {
                openNoteById(item.noteId, item.targetId);
            } else if (!TextUtils.isEmpty(item.targetId)) {
                // targetId 是评论 ID，去遍历笔记找到它
                openNoteByCommentId(item.targetId);
            } else {
                Toast.makeText(this, "未找到对应作品", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void openNoteById(String noteId, @Nullable String commentId) {
        if (noteId == null || noteId.isEmpty()) return;
        noteRepository.fetchNotes(null, null, null, null, new AliNoteRepository.NotesCallback() {
            @Override
            public void onSuccess(List<RegionItem> items) {
                if (items == null) return;
                for (RegionItem regionItem : items) {
                    if (regionItem != null && noteId.equals(regionItem.getNoteId())) {
                        runOnUiThread(() -> {
                            android.content.Intent intent = new android.content.Intent(NotificationActivity.this, NoteDetailActivity.class);
                            intent.putExtra(NoteDetailActivity.EXTRA_NOTE_DATA, regionItem);
                            if (commentId != null) {
                                intent.putExtra(NoteDetailActivity.EXTRA_TARGET_COMMENT_ID, commentId);
                            }
                            startActivity(intent);
                        });
                        return;
                    }
                }
            }

            @Override
            public void onError(@NonNull Throwable throwable) {
                runOnUiThread(() -> Toast.makeText(NotificationActivity.this, "加载作品失败", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void openNoteByCommentId(String commentId) {
        if (TextUtils.isEmpty(commentId)) return;
        noteRepository.fetchNotes(null, null, null, null, new AliNoteRepository.NotesCallback() {
            @Override
            public void onSuccess(List<RegionItem> items) {
                searchCommentInNotes(items, 0, commentId);
            }

            @Override
            public void onError(@NonNull Throwable throwable) {
                runOnUiThread(() -> Toast.makeText(NotificationActivity.this, "加载作品失败", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void searchCommentInNotes(List<RegionItem> items, int index, String commentId) {
        if (items == null || index >= items.size()) {
            runOnUiThread(() -> Toast.makeText(this, "未找到对应评论或作品", Toast.LENGTH_SHORT).show());
            return;
        }
        RegionItem note = items.get(index);
        if (note == null || TextUtils.isEmpty(note.getNoteId())) {
            searchCommentInNotes(items, index + 1, commentId);
            return;
        }
        noteRepository.fetchComments(note.getNoteId(), new AliNoteRepository.CommentsCallback() {
            @Override
            public void onSuccess(List<CommentItem> commentItems) {
                boolean matched = false;
                if (commentItems != null) {
                    for (CommentItem c : commentItems) {
                        if (c != null && commentId.equals(c.getId())) {
                            matched = true;
                            RegionItem targetNote = note;
                            runOnUiThread(() -> {
                                android.content.Intent intent = new android.content.Intent(NotificationActivity.this, NoteDetailActivity.class);
                                intent.putExtra(NoteDetailActivity.EXTRA_NOTE_DATA, targetNote);
                                intent.putExtra(NoteDetailActivity.EXTRA_TARGET_COMMENT_ID, commentId);
                                startActivity(intent);
                            });
                            break;
                        }
                    }
                }
                if (!matched) {
                    searchCommentInNotes(items, index + 1, commentId);
                }
            }

            @Override
            public void onError(@NonNull Throwable throwable) {
                searchCommentInNotes(items, index + 1, commentId);
            }
        });
    }

    private void loadNotifications() {
        api.getNotifications(1, 50).enqueue(new retrofit2.Callback<List<NotificationResponse>>() {
            @Override
            public void onResponse(retrofit2.Call<List<NotificationResponse>> call, retrofit2.Response<List<NotificationResponse>> response) {
                if (!response.isSuccessful() || response.body() == null) return;
                List<NotificationItem> list = new ArrayList<>();
        for (NotificationResponse r : response.body()) {
            list.add(new NotificationItem(
                    r.actorName != null ? r.actorName + " " + renderAction(r.type) : "消息",
                    r.message != null ? r.message : "",
                    r.time != null ? r.time : "",
                    r.actorAvatar,
                    r.targetType,
                    r.targetId,
                    r.noteId
            ));
        }
        runOnUiThread(() -> adapter.setData(list));
            }

            @Override
            public void onFailure(retrofit2.Call<List<NotificationResponse>> call, Throwable t) {
                // ignore
            }
        });
    }

    private String renderAction(String type) {
        if (type == null) return "";
        switch (type) {
            case "like_note": return "点赞了你的作品";
            case "comment_note": return "评论了你的作品";
            case "like_comment": return "点赞了你的评论";
            case "comment_comment": return "回复了你的评论";
            default: return "有新消息";
        }
    }

    static class NotificationItem {
        final String title;
        final String subtitle;
        final String time;
        final String avatarUrl;
        final String targetType;
        final String targetId;
        final String noteId;

        NotificationItem(String title, String subtitle, String time, String avatarUrl, String targetType, String targetId, String noteId) {
            this.title = title;
            this.subtitle = subtitle;
            this.time = time;
            this.avatarUrl = avatarUrl;
            this.targetType = targetType;
            this.targetId = targetId;
            this.noteId = noteId;
        }
    }
}
