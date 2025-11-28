package com.noworld.notemap.ui;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import android.view.inputmethod.InputMethodManager;

import com.amap.apis.cluster.demo.RegionItem;
import com.bumptech.glide.Glide;
import com.noworld.notemap.R;
import com.noworld.notemap.data.AliNoteRepository;
import com.noworld.notemap.data.LikedStore;
import com.noworld.notemap.data.TokenStore;
import com.noworld.notemap.data.UserStore;
import com.noworld.notemap.data.model.CommentItem;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

public class NoteDetailActivity extends AppCompatActivity {

    public static final String EXTRA_NOTE_DATA = "NOTE_DATA";

    private RegionItem mNote;

    private Toolbar toolbar;
    private ViewPager2 vpDetailPhotos;
    private TextView tvPhotoIndicator;
    private TextView tvDetailTitle;
    private TextView tvDetailDescription;
    private TextView tvDetailType;
    private TextView tvDetailLocation;

    private ImageView ivDetailAvatar;
    private TextView tvDetailAuthor;

    private TextView tvDetailTime;
    private RecyclerView rvComments;
    private TextView tvInputComment;
    private TextView tvCommentSectionTitle;

    private LinearLayout layoutLike;
    private ImageView ivDetailLike;
    private TextView tvLikeCount;
    private TextView tvCommentCount;
    private LinearLayout layoutComment;

    private AliNoteRepository noteRepository;
    private LikedStore likedStore;
    private UserStore userStore;
    private TokenStore tokenStore;

    private final List<String> photoUrls = new ArrayList<>();
    private int currentIndex = 0;

    private final List<CommentItem> rawComments = new ArrayList<>();
    private final List<CommentItem> comments = new ArrayList<>();
    private CommentAdapter commentAdapter;
    private CommentItem replyTarget;
    private BottomSheetDialog commentDialog;
    private EditText commentInput;
    private final java.util.Map<String, Integer> replyShownCount = new java.util.HashMap<>();
    private static final String TAG = "NoteDetailActivity";

    /**
     * 计算当前评论应该归属的“根父评论”ID（顶级评论 ID）。
     * 如果 parentId 指向的是另一条子评论，则逐级向上找到顶级。
     */
    private String resolveRootParentId(CommentItem item, java.util.Map<String, CommentItem> idMap) {
        if (item == null) return null;
        String pid = normalizeParentId(item.getParentId());
        if (pid == null) return null;
        String current = pid;
        int guard = 0;
        while (current != null && guard < 10_000) {
            CommentItem p = idMap.get(current);
            if (p == null) break;
            String next = normalizeParentId(p.getParentId());
            if (next == null) break;
            current = next;
            guard++;
        }
        return current;
    }

    // 【新增】标记当前用户是否是作者
    private boolean isAuthor = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_detail);

        noteRepository = AliNoteRepository.getInstance(this);
        likedStore = LikedStore.getInstance(this);
        userStore = UserStore.getInstance(this);
        tokenStore = TokenStore.getInstance(this);

        if (getIntent().hasExtra(EXTRA_NOTE_DATA)) {
            mNote = (RegionItem) getIntent().getSerializableExtra(EXTRA_NOTE_DATA);
        }

        initView();
        initToolbar();
        initEvent();

        if (mNote != null) {
            checkIsAuthor(); // 检查权限
            populateData();
        } else {
            Toast.makeText(this, "加载笔记数据失败", Toast.LENGTH_SHORT).show();
        }
    }

    private void checkIsAuthor() {
        String currentUid = userStore.extractUidFromToken(tokenStore.getToken());
        if (currentUid == null) currentUid = userStore.getUid();

        // 比较当前用户ID和作者ID
        // 注意：RegionItem 里的 authorId 可能是 String
        if (currentUid != null && mNote.getAuthorId() != null) {
            isAuthor = currentUid.equals(mNote.getAuthorId());
        }

        // 调用 invalidateOptionsMenu 会触发 onCreateOptionsMenu 重新绘制菜单
        invalidateOptionsMenu();
    }

    // ===========================================
    // 【核心新增】菜单处理逻辑
    // ===========================================

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // 只有作者本人才能看到这三个点
        if (isAuthor) {
            getMenuInflater().inflate(R.menu.menu_note_detail, menu);
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // 动态改变菜单文字
        if (isAuthor) {
            MenuItem privacyItem = menu.findItem(R.id.action_privacy);
            if (privacyItem != null) {
                if (mNote.isPrivate()) {
                    privacyItem.setTitle("设为公开笔记");
                } else {
                    privacyItem.setTitle("设为仅自己可见");
                }
            }
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        } else if (item.getItemId() == R.id.action_privacy) {
            handleTogglePrivacy();
            return true;
        } else if (item.getItemId() == R.id.action_delete) {
            handleDeleteNote();
            return true;
        } else if (item.getItemId() == R.id.action_edit) {
            handleEditNote();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // 处理：修改可见性
    private void handleTogglePrivacy() {
        boolean newStatus = !mNote.isPrivate(); // 目标状态
        noteRepository.setNotePrivacy(mNote.getNoteId(), newStatus, new AliNoteRepository.SimpleCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    mNote.setPrivate(newStatus);
                    // 刷新菜单文字
                    invalidateOptionsMenu();
                    String msg = newStatus ? "已设为仅自己可见" : "已设为公开";
                    Toast.makeText(NoteDetailActivity.this, msg, Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onError(Throwable t) {
                runOnUiThread(() -> Toast.makeText(NoteDetailActivity.this, "设置失败: " + t.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }

    // 处理：删除笔记
    private void handleDeleteNote() {
        new AlertDialog.Builder(this)
                .setTitle("删除笔记")
                .setMessage("确定要删除这条笔记吗？删除后不可恢复。")
                .setPositiveButton("删除", (dialog, which) -> {
                    noteRepository.deleteNote(mNote.getNoteId(), new AliNoteRepository.SimpleCallback() {
                        @Override
                        public void onSuccess() {
                            runOnUiThread(() -> {
                                Toast.makeText(NoteDetailActivity.this, "删除成功", Toast.LENGTH_SHORT).show();
                                finish(); // 关闭详情页，返回列表
                            });
                        }

                        @Override
                        public void onError(Throwable t) {
                            runOnUiThread(() -> Toast.makeText(NoteDetailActivity.this, "删除失败: " + t.getMessage(), Toast.LENGTH_SHORT).show());
                        }
                    });
                })
                .setNegativeButton("取消", null)
                .show();
    }

    // 处理：修改笔记内容
    private void handleEditNote() {
        if (mNote == null) return;

        Intent intent = new Intent(this, AddNoteActivity.class);

        // 【关键】传递当前的笔记数据，告诉 AddNoteActivity 进入编辑模式
        intent.putExtra(AddNoteActivity.EXTRA_EDIT_NOTE_DATA, mNote);

        // 使用 startActivityForResult 或 ActivityResultLauncher 来等待编辑结果
        // 确保编辑完成后，当前详情页能刷新
        startActivity(intent); // 简化处理，暂时直接启动

        // ⚠️ 最佳实践：此处应使用 ActivityResultLauncher
        // 待您确认功能可用后，可以替换为 ActivityResultLauncher 并在 onResume 刷新数据
    }

    // ===========================================

    private void initView() {
        toolbar = findViewById(R.id.toolbar_note_detail);
        vpDetailPhotos = findViewById(R.id.vp_detail_photos);
        tvPhotoIndicator = findViewById(R.id.tv_photo_indicator);
        tvDetailTitle = findViewById(R.id.tv_detail_title);
        tvDetailDescription = findViewById(R.id.tv_detail_description);
        tvDetailType = findViewById(R.id.tv_detail_type);
        tvDetailLocation = findViewById(R.id.tv_detail_location);

        ivDetailAvatar = findViewById(R.id.iv_detail_avatar);
        tvDetailAuthor = findViewById(R.id.tv_detail_author);

        tvDetailTime = findViewById(R.id.tv_detail_time);
        rvComments = findViewById(R.id.rv_comments);
        tvInputComment = findViewById(R.id.tv_input_comment);
        tvCommentSectionTitle = findViewById(R.id.tv_comment_section_title);

        layoutLike = findViewById(R.id.layout_like);
        layoutComment = findViewById(R.id.layout_comment);
        ivDetailLike = findViewById(R.id.iv_detail_like);
        tvLikeCount = findViewById(R.id.tv_detail_like_count);
        tvCommentCount = findViewById(R.id.tv_detail_comment_count);

        if (rvComments != null) {
            rvComments.setLayoutManager(new LinearLayoutManager(this));
            rvComments.setNestedScrollingEnabled(false);
            commentAdapter = new CommentAdapter(comments);
            commentAdapter.setOnCommentActionListener(new CommentAdapter.OnCommentActionListener() {
                @Override
                public void onReply(CommentItem item) { onReplyClicked(item); }

                @Override
                public void onToggleReplies(String parentId, boolean expand) { NoteDetailActivity.this.onToggleReplies(parentId, expand); }

                @Override
                public void onLike(CommentItem item) { onCommentLike(item); }

                @Override
                public void onLongPress(CommentItem item) { onCommentLongPress(item); }
            });
            rvComments.setAdapter(commentAdapter);
        }
    }

    private void initToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("笔记详情");
        }
    }

    private void initEvent() {
        if (tvInputComment != null) {
            tvInputComment.setOnClickListener(v -> showCommentSheet());
        }
        if (layoutLike != null) {
            layoutLike.setOnClickListener(v -> handleLikeClick());
        }
        if (layoutComment != null) {
            layoutComment.setOnClickListener(v -> showCommentSheet());
        }
    }

    private void populateData() {
        tvDetailTitle.setText(mNote.getTitle());
        tvDetailDescription.setText(mNote.getDescription());
        tvDetailType.setText(mNote.getNoteType() != null ? "#" + mNote.getNoteType() : "#推荐");
        tvDetailLocation.setText(mNote.getLocationName());

        if (tvDetailAuthor != null) {
            tvDetailAuthor.setText(mNote.getAuthorName());
        }
        if (ivDetailAvatar != null) {
            Glide.with(this)
                    .load(mNote.getAuthorAvatarUrl())
                    .placeholder(R.drawable.ic_profile)
                    .error(R.drawable.ic_profile)
                    .into(ivDetailAvatar);
        }

        if (tvDetailTime != null) {
            String time = mNote.getCreateTime();
            // 【关键修改】如果时间为空，显示空字符串而不是旧的硬编码日期
            tvDetailTime.setText("发布于 " + (time != null && !time.isEmpty() ? time : "未知时间"));
        }

        refreshLikeState();

        photoUrls.clear();
        if (mNote.getImageUrls() != null && !mNote.getImageUrls().isEmpty()) {
            photoUrls.addAll(mNote.getImageUrls());
        } else if (mNote.getPhotoUrl() != null) {
            photoUrls.add(mNote.getPhotoUrl());
        }
        if (photoUrls.isEmpty()) {
            photoUrls.add(null);
        }
        setupViewPager();

        loadComments();
    }

    private void refreshLikeState() {
        if (mNote == null) return;
        String uid = userStore.extractUidFromToken(tokenStore.getToken());
        if (uid == null) uid = userStore.getUid();

        Set<String> likedIds = likedStore.getLikedIds(uid);
        boolean isLiked = likedIds.contains(mNote.getNoteId());
        mNote.setLikedByCurrentUser(isLiked);

        Integer storedCount = likedStore.getLikeCount(mNote.getNoteId());
        if (storedCount != null) {
            mNote.setLikeCount(storedCount);
        }

        tvLikeCount.setText(String.valueOf(mNote.getLikeCount()));
        ivDetailLike.setImageResource(isLiked ? R.drawable.ic_like_filled : R.drawable.ic_like);
    }

    private void handleLikeClick() {
        if (tokenStore.getToken() == null || tokenStore.getToken().isEmpty()) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            return;
        }

        noteRepository.toggleLike(mNote, new AliNoteRepository.LikeCallback() {
            @Override
            public void onResult(boolean liked, int likeCount) {
                mNote.setLikedByCurrentUser(liked);
                mNote.setLikeCount(likeCount);

                String uid = userStore.extractUidFromToken(tokenStore.getToken());
                likedStore.toggle(uid, mNote.getNoteId(), liked);
                likedStore.saveLikeCount(mNote.getNoteId(), likeCount);

                runOnUiThread(() -> {
                    tvLikeCount.setText(String.valueOf(likeCount));
                    ivDetailLike.setImageResource(liked ? R.drawable.ic_like_filled : R.drawable.ic_like);
                });
            }

            @Override
            public void onRequireLogin() {
                runOnUiThread(() -> {
                    Toast.makeText(NoteDetailActivity.this, "登录已过期", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(NoteDetailActivity.this, LoginActivity.class));
                });
            }

            @Override
            public void onError(@NonNull Throwable e) {
                runOnUiThread(() -> Toast.makeText(NoteDetailActivity.this, "操作失败: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void loadComments() {
        if (mNote == null) return;
        noteRepository.fetchComments(mNote.getNoteId(), new AliNoteRepository.CommentsCallback() {
            @Override
            public void onSuccess(List<CommentItem> list) {
                runOnUiThread(() -> {
                    replyShownCount.clear();
                    rawComments.clear();
                    if (list != null) {
                        rawComments.addAll(list);
                    }
                    Log.d(TAG, "loadComments success raw size=" + rawComments.size());
                    comments.clear();
                    comments.addAll(buildDisplayList(rawComments));
                    if (commentAdapter != null) {
                        commentAdapter.updateData(comments);
                    }
                    updateCommentCounter();
                });
            }

            @Override
            public void onError(@NonNull Throwable throwable) {
                runOnUiThread(() -> Toast.makeText(NoteDetailActivity.this, "加载评论失败: " + throwable.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void updateCommentCounter() {
        int count = rawComments.size();
        if (tvCommentCount != null) {
            tvCommentCount.setText(String.valueOf(count));
        }
        if (tvCommentSectionTitle != null) {
            tvCommentSectionTitle.setText("共 " + count + " 条评论");
        }
    }

    private List<CommentItem> buildDisplayList(List<CommentItem> original) {
        List<CommentItem> parents = new ArrayList<>();
        java.util.Map<String, CommentItem> idMap = new java.util.HashMap<>();
        for (CommentItem c : original) {
            if (c.getId() != null) {
                idMap.put(c.getId(), c);
            }
        }
        LinkedHashMap<String, List<CommentItem>> childMap = new LinkedHashMap<>();
        for (CommentItem item : original) {
            String pid = resolveRootParentId(item, idMap);
            if (pid != null) {
                childMap.computeIfAbsent(pid, k -> new ArrayList<>()).add(item);
            } else {
                parents.add(item);
            }
        }
        Log.d(TAG, "buildDisplayList parents=" + parents.size() + " childGroups=" + childMap.size());
        List<CommentItem> result = new ArrayList<>();
        for (CommentItem parent : parents) {
            result.add(parent);
            List<CommentItem> childList = childMap.get(parent.getId());
            if (childList == null || childList.isEmpty()) continue;
            int shown = replyShownCount.getOrDefault(parent.getId(), 0);
            // 初始折叠：如果子评论>1且尚未记录，则设为0（不展示）
            if (!replyShownCount.containsKey(parent.getId()) && childList.size() > 1) {
                shown = 0;
            }
            int threshold = 3;
            int toShow = Math.min(shown, childList.size());
            if (childList.size() <= 1) {
                result.addAll(childList);
                Log.d(TAG, "parent " + parent.getId() + " children=" + childList.size() + " show all (<=1)");
                continue;
            }
            if (toShow > 0) {
                result.addAll(childList.subList(0, toShow));
            }
            int remaining = childList.size() - toShow;
            if (remaining > 0) {
                result.add(new CommentItem(
                        "more_" + parent.getId(),
                        "",
                        "",
                        "",
                        null,
                        parent.getId(),
                        null,
                        null,
                        0,
                        false,
                        true,
                        remaining
                ));
            }
            Log.d(TAG, "parent " + parent.getId() + " children=" + childList.size() + " shown=" + toShow + " remaining=" + remaining);
        }
        Log.d(TAG, "buildDisplayList result size=" + result.size());
        return result;
    }

    private void rebuildDisplayComments() {
        comments.clear();
        comments.addAll(buildDisplayList(rawComments));
        if (commentAdapter != null) {
            commentAdapter.updateData(comments);
        }
        updateCommentCounter();
    }

    private String normalizeParentId(String pid) {
        if (pid == null) return null;
        String t = pid.trim();
        if (t.isEmpty() || "0".equals(t) || "null".equalsIgnoreCase(t)) return null;
        return t;
    }

    private boolean isOwnComment(CommentItem item) {
        if (item == null) return false;
        String currentUid = userStore.extractUidFromToken(tokenStore.getToken());
        if (currentUid == null) currentUid = userStore.getUid();
        if (item.getAuthorId() != null && currentUid != null) {
            return currentUid.equals(String.valueOf(item.getAuthorId()));
        }
        String currentName = userStore.getUsername();
        return currentName != null && currentName.equals(item.getUserName());
    }

    private void onCommentLongPress(CommentItem item) {
        if (!isOwnComment(item)) return;
        new AlertDialog.Builder(this)
                .setTitle("删除评论")
                .setMessage("确定要删除这条评论吗？")
                .setPositiveButton("删除", (d, w) -> deleteComment(item))
                .setNegativeButton("取消", null)
                .show();
    }

    private void deleteComment(CommentItem item) {
        noteRepository.deleteComment(item.getId(), new AliNoteRepository.CommentDeleteCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    removeCommentById(item.getId());
                    rebuildDisplayComments();
                    Toast.makeText(NoteDetailActivity.this, "已删除", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onRequireLogin() {
                runOnUiThread(() -> {
                    Toast.makeText(NoteDetailActivity.this, "请先登录再删除", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(NoteDetailActivity.this, LoginActivity.class));
                });
            }

            @Override
            public void onError(@NonNull Throwable throwable) {
                runOnUiThread(() -> Toast.makeText(NoteDetailActivity.this, "删除失败: " + throwable.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void removeCommentById(String id) {
        if (id == null) return;
        java.util.Iterator<CommentItem> it = rawComments.iterator();
        while (it.hasNext()) {
            CommentItem c = it.next();
            String root = resolveRootParentId(c, toIdMap(rawComments));
            if (id.equals(c.getId()) || id.equals(root)) {
                it.remove();
            }
        }
    }

    private java.util.Map<String, CommentItem> toIdMap(List<CommentItem> list) {
        java.util.Map<String, CommentItem> map = new java.util.HashMap<>();
        for (CommentItem c : list) {
            if (c.getId() != null) {
                map.put(c.getId(), c);
            }
        }
        return map;
    }

    private boolean ensureLoggedInForComment() {
        String token = tokenStore.getToken();
        if (token == null || token.isEmpty()) {
            Toast.makeText(this, "请先登录再发表评论", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            return false;
        }
        return true;
    }

    private void showCommentSheet() {
        if (mNote == null) return;
        if (!ensureLoggedInForComment()) return;

        if (commentDialog == null) {
            commentDialog = new BottomSheetDialog(this);
            View sheet = getLayoutInflater().inflate(R.layout.dialog_comment_input, null, false);
            commentInput = sheet.findViewById(R.id.et_comment_input);
            View btnSend = sheet.findViewById(R.id.btn_comment_send);
            View btnClose = sheet.findViewById(R.id.iv_comment_close);

            btnSend.setOnClickListener(v -> {
                if (commentInput == null) return;
                String content = commentInput.getText().toString().trim();
                if (TextUtils.isEmpty(content)) {
                    Toast.makeText(NoteDetailActivity.this, "评论内容不能为空", Toast.LENGTH_SHORT).show();
                    return;
                }
                submitComment(content);
            });
            btnClose.setOnClickListener(v -> commentDialog.dismiss());
            commentDialog.setContentView(sheet);
        }

        if (commentInput != null) {
            String hint = replyTarget != null ? ("回复 " + replyTarget.getUserName()) : "友善评论，理性发言";
            commentInput.setHint(hint);
            commentInput.setText("");
            commentInput.requestFocus();
            commentInput.post(() -> {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) imm.showSoftInput(commentInput, InputMethodManager.SHOW_IMPLICIT);
            });
        }
        if (!commentDialog.isShowing()) {
            commentDialog.show();
        }
    }

    private void submitComment(String content) {
        if (mNote == null) return;
        String parentId;
        if (replyTarget != null) {
            // 统一指向顶级父评论，避免多级回复丢失
            String targetParent = normalizeParentId(replyTarget.getParentId());
            parentId = targetParent != null ? targetParent : replyTarget.getId();
        } else {
            parentId = null;
        }
        noteRepository.addComment(mNote.getNoteId(), content, parentId, new AliNoteRepository.AddCommentCallback() {
            @Override
            public void onSuccess(CommentItem newComment) {
                runOnUiThread(() -> {
                    CommentItem displayItem = newComment;
                    if (replyTarget != null && (newComment.getReplyToUserName() == null || newComment.getReplyToUserName().isEmpty())) {
                        displayItem = new CommentItem(
                                newComment.getId(),
                                newComment.getUserName(),
                                newComment.getContent(),
                                newComment.getTime(),
                                newComment.getAvatarUrl(),
                                parentId,
                                replyTarget.getUserName(),
                                newComment.getAuthorId(),
                                newComment.getLikeCount(),
                                newComment.isLiked(),
                                false,
                                0
                        );
                    }
                    rawComments.add(0, displayItem);
                    rebuildDisplayComments();
                    Toast.makeText(NoteDetailActivity.this, "评论成功", Toast.LENGTH_SHORT).show();
                    replyTarget = null;
                    if (commentInput != null) {
            commentInput.setText("");
        }
        if (commentDialog != null && commentDialog.isShowing()) {
            commentDialog.dismiss();
        }
        // 保持当前父评论的展开状态，不强制展开
        Log.d(TAG, "submitComment displayItem parentId=" + displayItem.getParentId() + " isReply=" + displayItem.isReply());
    });
}

            @Override
            public void onRequireLogin() {
                runOnUiThread(() -> {
                    Toast.makeText(NoteDetailActivity.this, "登录已过期，请重新登录", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(NoteDetailActivity.this, LoginActivity.class));
                    replyTarget = null;
                });
            }

            @Override
            public void onError(@NonNull Throwable throwable) {
                runOnUiThread(() -> Toast.makeText(NoteDetailActivity.this, "评论失败: " + throwable.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void onReplyClicked(CommentItem item) {
        if (item.isMoreIndicator()) {
            onToggleReplies(item.getParentId(), true);
            return;
        }
        this.replyTarget = item;
        showCommentSheet();
    }

    private void onToggleReplies(String parentId, boolean expand) {
        if (parentId == null) return;
        int current = replyShownCount.getOrDefault(parentId, 0);
        // 若当前为0且子评论超过1，意味着初始折叠，第一次点击显示3条
        replyShownCount.put(parentId, current + 3);
        rebuildDisplayComments();
    }

    private void onCommentLike(CommentItem item) {
        if (item == null) return;
        noteRepository.toggleCommentLike(item.getId(), new AliNoteRepository.CommentLikeCallback() {
            @Override
            public void onResult(boolean liked, int likeCount) {
                runOnUiThread(() -> {
                    for (int i = 0; i < rawComments.size(); i++) {
                        CommentItem c = rawComments.get(i);
                        if (c.getId().equals(item.getId())) {
                            rawComments.set(i, new CommentItem(
                                    c.getId(),
                                    c.getUserName(),
                                    c.getContent(),
                                    c.getTime(),
                                    c.getAvatarUrl(),
                                    c.getParentId(),
                                    c.getReplyToUserName(),
                                    c.getAuthorId(),
                                    likeCount,
                                    liked
                            ));
                            break;
                        }
                    }
                    rebuildDisplayComments();
                });
            }

            @Override
            public void onRequireLogin() {
                runOnUiThread(() -> {
                    Toast.makeText(NoteDetailActivity.this, "请先登录再点赞评论", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(NoteDetailActivity.this, LoginActivity.class));
                });
            }

            @Override
            public void onError(@NonNull Throwable throwable) {
                runOnUiThread(() -> Toast.makeText(NoteDetailActivity.this, "操作失败: " + throwable.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void openFullImage() {
        String photoUrl = photoUrls.get(currentIndex);
        if (photoUrl == null || photoUrl.isEmpty()) return;
        Intent intent = new Intent(this, PictureActivity.class);
        intent.putExtra(PictureActivity.EXTRA_IMAGE_URL, photoUrl);
        intent.putExtra(PictureActivity.EXTRA_AUTHOR_NAME, mNote.getAuthorName());
        startActivity(intent);
    }

    private void setupViewPager() {
        vpDetailPhotos.setAdapter(new PhotoPagerAdapter(photoUrls, this::openFullImage));
        updateIndicator(0);
        vpDetailPhotos.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                currentIndex = position;
                updateIndicator(position);
            }
        });
    }

    private void updateIndicator(int position) {
        tvPhotoIndicator.setText((position + 1) + "/" + photoUrls.size());
    }

    private class PhotoPagerAdapter extends RecyclerView.Adapter<PhotoPagerAdapter.PhotoVH> {
        private final List<String> data;
        private final Runnable onClick;

        PhotoPagerAdapter(List<String> data, Runnable onClick) {
            this.data = data;
            this.onClick = onClick;
        }

        @NonNull
        @Override
        public PhotoVH onCreateViewHolder(@NonNull android.view.ViewGroup parent, int viewType) {
            ImageView iv = new ImageView(parent.getContext());
            iv.setLayoutParams(new android.view.ViewGroup.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT));

            // 【核心修改】
            // 原来是 CENTER_CROP (填满并裁切)，现在改为 FIT_CENTER (完整显示)
            iv.setScaleType(ImageView.ScaleType.FIT_CENTER);

            // 可选：设置一个背景色（比如黑色或灰色），这样如果图片比例和容器不一样，留白部分不会太突兀
            // iv.setBackgroundColor(android.graphics.Color.BLACK);

            return new PhotoVH(iv);
        }

        @Override
        public void onBindViewHolder(@NonNull PhotoVH holder, int position) {
            String url = data.get(position);
            Glide.with(holder.imageView.getContext())
                    .load(url)
                    .placeholder(R.drawable.ic_car)
                    .error(R.drawable.ic_car)
                    .into(holder.imageView);
        }

        @Override
        public int getItemCount() { return data.size(); }

        class PhotoVH extends RecyclerView.ViewHolder {
            public ImageView imageView;
            public PhotoVH(@NonNull ImageView itemView) {
                super(itemView);
                this.imageView = itemView;

                // 【最终修正：恢复原始的点击逻辑】
                itemView.setOnClickListener(v -> {
                    // 核心：调用 PhotoPagerAdapter 构造函数中传入的 Runnable (即 openFullImage())
                    PhotoPagerAdapter.this.onClick.run();
                });
            }
        }
    }
}
