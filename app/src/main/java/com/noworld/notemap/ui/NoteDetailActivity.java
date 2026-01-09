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
import android.graphics.drawable.Drawable;
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
import androidx.core.widget.NestedScrollView;
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
    public static final String EXTRA_TARGET_COMMENT_ID = "TARGET_COMMENT_ID";

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
    private NestedScrollView nestedScrollView;

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
    private String targetCommentId;
    private boolean hasScrolledToTargetComment = false;
    private int targetCommentScrollAttempts = 0;
    private int highlightAttempts = 0;
    private static final String TAG = "NoteDetailActivity";

    /**
     * Resolve the root parent comment ID (top-level).
     * If parentId points to a child comment, walk up until the top-level parent.
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

    // Mark whether the current user is the author.
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
        if (getIntent().hasExtra(EXTRA_TARGET_COMMENT_ID)) {
            targetCommentId = getIntent().getStringExtra(EXTRA_TARGET_COMMENT_ID);
        }

        initView();
        initToolbar();
        initEvent();

        if (mNote != null) {
            checkIsAuthor(); // Check permissions.
            populateData();
        } else {
            Toast.makeText(this, "Failed to load note data", Toast.LENGTH_SHORT).show();
        }
    }

    private void checkIsAuthor() {
        String currentUid = userStore.extractUidFromToken(tokenStore.getToken());
        if (currentUid == null) currentUid = userStore.getUid();

        // Compare current user ID with author ID.
        // Note: authorId in RegionItem may be a String.
        if (currentUid != null && mNote.getAuthorId() != null) {
            isAuthor = currentUid.equals(mNote.getAuthorId());
        }

        // invalidateOptionsMenu triggers onCreateOptionsMenu to redraw the menu.
        invalidateOptionsMenu();
    }

    // ===========================================
    // Menu handling logic.
    // ===========================================

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Only the author can see these options.
        if (isAuthor) {
            getMenuInflater().inflate(R.menu.menu_note_detail, menu);
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // Update menu text dynamically.
        if (isAuthor) {
            MenuItem privacyItem = menu.findItem(R.id.action_privacy);
            if (privacyItem != null) {
                if (mNote.isPrivate()) {
                    privacyItem.setTitle("Make public");
                } else {
                    privacyItem.setTitle("Make private");
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

    // Handle visibility toggle.
    private void handleTogglePrivacy() {
        boolean newStatus = !mNote.isPrivate(); // Target status.
        noteRepository.setNotePrivacy(mNote.getNoteId(), newStatus, new AliNoteRepository.SimpleCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    mNote.setPrivate(newStatus);
                    // Refresh menu text.
                    invalidateOptionsMenu();
                    String msg = newStatus ? "Set to private" : "Set to public";
                    Toast.makeText(NoteDetailActivity.this, msg, Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onError(Throwable t) {
                runOnUiThread(() -> Toast.makeText(NoteDetailActivity.this, "Failed to update: " + t.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }

    // Handle note deletion.
    private void handleDeleteNote() {
        new AlertDialog.Builder(this)
                .setTitle("Delete note")
                .setMessage("Delete this note? This cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    noteRepository.deleteNote(mNote.getNoteId(), new AliNoteRepository.SimpleCallback() {
                        @Override
                        public void onSuccess() {
                            runOnUiThread(() -> {
                                Toast.makeText(NoteDetailActivity.this, "Deleted", Toast.LENGTH_SHORT).show();
                                finish(); // Close detail and return to list.
                            });
                        }

                        @Override
                        public void onError(Throwable t) {
                            runOnUiThread(() -> Toast.makeText(NoteDetailActivity.this, "Delete failed: " + t.getMessage(), Toast.LENGTH_SHORT).show());
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // Handle note editing.
    private void handleEditNote() {
        if (mNote == null) return;

        Intent intent = new Intent(this, AddNoteActivity.class);

        // Pass the note data to enter edit mode in AddNoteActivity.
        intent.putExtra(AddNoteActivity.EXTRA_EDIT_NOTE_DATA, mNote);

        // Use startActivityForResult or ActivityResultLauncher to await edit results.
        // Ensure the detail page refreshes after editing.
        startActivity(intent); // Simplified: launch directly for now.

        // Best practice: use ActivityResultLauncher here.
        // Once confirmed, replace and refresh data in onResume.
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
        nestedScrollView = findViewById(R.id.nested_note_detail);

        ivDetailAvatar = findViewById(R.id.iv_detail_avatar);
        tvDetailAuthor = findViewById(R.id.tv_detail_author);
        ivDetailAvatar.setOnClickListener(v -> openAuthorProfile());
        tvDetailAuthor.setOnClickListener(v -> openAuthorProfile());

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
            getSupportActionBar().setTitle("Note Details");
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
        tvDetailType.setText("#" + toEnglishTag(mNote.getNoteType()));
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
            ivDetailAvatar.setOnClickListener(v -> openAuthorProfile());
        }

        if (tvDetailTime != null) {
            String time = mNote.getCreateTime();
            // If time is empty, show an empty string instead of a hardcoded date.
            tvDetailTime.setText("Published at " + (time != null && !time.isEmpty() ? time : "Unknown time"));
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

    private void openAuthorProfile() {
        Intent intent = new Intent(this, ProfileActivity.class);
        intent.putExtra(ProfileActivity.EXTRA_USER_ID, mNote.getAuthorId());
        intent.putExtra(ProfileActivity.EXTRA_USER_NAME, mNote.getAuthorName());
        intent.putExtra(ProfileActivity.EXTRA_USER_AVATAR, mNote.getAuthorAvatarUrl());
        startActivity(intent);
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
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(NoteDetailActivity.this, "Login expired", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(NoteDetailActivity.this, LoginActivity.class));
                });
            }

            @Override
            public void onError(@NonNull Throwable e) {
                runOnUiThread(() -> Toast.makeText(NoteDetailActivity.this, "Operation failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
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
                    maybeScrollToTargetComment();
                });
            }

            @Override
            public void onError(@NonNull Throwable throwable) {
                runOnUiThread(() -> Toast.makeText(NoteDetailActivity.this, "Failed to load comments: " + throwable.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void updateCommentCounter() {
        int count = rawComments.size();
        if (tvCommentCount != null) {
            tvCommentCount.setText(String.valueOf(count));
        }
        if (tvCommentSectionTitle != null) {
            tvCommentSectionTitle.setText("Comments: " + count);
        }
    }

    private String toEnglishTag(String raw) {
        if (raw == null || raw.trim().isEmpty()) return "Recommended";
        switch (raw.trim()) {
            case "种草": return "Recommendation";
            case "攻略": return "Guide";
            case "测评": return "Review";
            case "分享": return "Share";
            case "合集": return "Collection";
            case "教程": return "Tutorial";
            case "开箱": return "Unboxing";
            case "探店": return "Store visit";
            case "推荐": return "Recommended";
            case "美食": return "Food";
            case "风景": return "Scenery";
            default: return raw.trim();
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
            // Initial collapse: if children > 1 and not recorded, set to 0 (hidden).
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
        maybeScrollToTargetComment();
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
                .setTitle("Delete comment")
                .setMessage("Delete this comment?")
                .setPositiveButton("Delete", (d, w) -> deleteComment(item))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteComment(CommentItem item) {
        noteRepository.deleteComment(item.getId(), new AliNoteRepository.CommentDeleteCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    removeCommentById(item.getId());
                    rebuildDisplayComments();
                    Toast.makeText(NoteDetailActivity.this, "Deleted", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onRequireLogin() {
                runOnUiThread(() -> {
                    Toast.makeText(NoteDetailActivity.this, "Please log in before deleting", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(NoteDetailActivity.this, LoginActivity.class));
                });
            }

            @Override
            public void onError(@NonNull Throwable throwable) {
                runOnUiThread(() -> Toast.makeText(NoteDetailActivity.this, "Delete failed: " + throwable.getMessage(), Toast.LENGTH_SHORT).show());
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

    private void maybeScrollToTargetComment() {
        if (hasScrolledToTargetComment || targetCommentId == null || comments.isEmpty() || rawComments.isEmpty() || rvComments == null) {
            return;
        }
        scrollToCommentSection();
        java.util.Map<String, CommentItem> idMap = toIdMap(rawComments);
        CommentItem target = idMap.get(targetCommentId);
        if (target == null) {
            return;
        }
        String rootId = resolveRootParentId(target, idMap);
        if (rootId != null && target.isReply()) {
            int childCount = countRepliesForRoot(rootId, idMap);
            replyShownCount.put(rootId, childCount);
            comments.clear();
            comments.addAll(buildDisplayList(rawComments));
            if (commentAdapter != null) {
                commentAdapter.updateData(comments);
            }
        }
        int index = findCommentIndex(targetCommentId);
        if (index >= 0) {
            hasScrolledToTargetComment = true;
            rvComments.post(() -> {
                RecyclerView.LayoutManager lm = rvComments.getLayoutManager();
                if (lm instanceof LinearLayoutManager) {
                    int offsetPx = (int) (rvComments.getResources().getDisplayMetrics().density * 12);
                    ((LinearLayoutManager) lm).scrollToPositionWithOffset(index, offsetPx);
                } else {
                    rvComments.scrollToPosition(index);
                }
                highlightCommentItem(index);
            });
        } else if (targetCommentScrollAttempts < 3) {
            targetCommentScrollAttempts++;
            // Retry a few times until layout/data is ready.
            rvComments.postDelayed(this::maybeScrollToTargetComment, 120);
        }
    }

    private void scrollToCommentSection() {
        if (nestedScrollView == null || tvCommentSectionTitle == null) return;
        nestedScrollView.post(() -> nestedScrollView.smoothScrollTo(0, tvCommentSectionTitle.getTop()));
    }

    private void highlightCommentItem(int position) {
        if (rvComments == null) return;
        rvComments.postDelayed(() -> {
            RecyclerView.ViewHolder vh = rvComments.findViewHolderForAdapterPosition(position);
            if (vh == null) {
                if (highlightAttempts < 5) {
                    highlightAttempts++;
                    highlightCommentItem(position);
                }
                return;
            }
            highlightAttempts = 0;
            View targetView = vh.itemView;
            Drawable originalBg = targetView.getBackground();
            int highlightColor = 0x33FFC107; // Semi-transparent yellow.
            targetView.setBackgroundColor(highlightColor);
            targetView.setAlpha(0.7f);
            targetView.animate().alpha(1f).setDuration(200).setStartDelay(200).withEndAction(() ->
                    targetView.postDelayed(() -> {
                        targetView.animate().alpha(0.85f).setDuration(200).withEndAction(() -> {
                            targetView.setBackground(originalBg);
                            targetView.setAlpha(1f);
                        }).start();
                    }, 400)
            ).start();
        }, 120);
    }

    private int findCommentIndex(String id) {
        if (id == null) return -1;
        for (int i = 0; i < comments.size(); i++) {
            if (id.equals(comments.get(i).getId())) {
                return i;
            }
        }
        return -1;
    }

    private int countRepliesForRoot(String rootId, java.util.Map<String, CommentItem> idMap) {
        if (rootId == null) return 0;
        int count = 0;
        for (CommentItem c : rawComments) {
            if (rootId.equals(resolveRootParentId(c, idMap))) {
                count++;
            }
        }
        return count;
    }

    private boolean ensureLoggedInForComment() {
        String token = tokenStore.getToken();
        if (token == null || token.isEmpty()) {
            Toast.makeText(this, "Please log in before commenting", Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(NoteDetailActivity.this, "Comment cannot be empty", Toast.LENGTH_SHORT).show();
                    return;
                }
                submitComment(content);
            });
            btnClose.setOnClickListener(v -> commentDialog.dismiss());
            commentDialog.setContentView(sheet);
        }

        if (commentInput != null) {
            String hint = replyTarget != null ? ("Reply to " + replyTarget.getUserName()) : "Be kind and stay rational";
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
            // Normalize to top-level parent to avoid multi-level reply loss.
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
                    Toast.makeText(NoteDetailActivity.this, "Comment posted", Toast.LENGTH_SHORT).show();
                    replyTarget = null;
                    if (commentInput != null) {
            commentInput.setText("");
        }
        if (commentDialog != null && commentDialog.isShowing()) {
            commentDialog.dismiss();
        }
        // Keep current parent expansion state; do not force expand.
        Log.d(TAG, "submitComment displayItem parentId=" + displayItem.getParentId() + " isReply=" + displayItem.isReply());
    });
}

            @Override
            public void onRequireLogin() {
                runOnUiThread(() -> {
                    Toast.makeText(NoteDetailActivity.this, "Login expired, please log in again", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(NoteDetailActivity.this, LoginActivity.class));
                    replyTarget = null;
                });
            }

            @Override
            public void onError(@NonNull Throwable throwable) {
                runOnUiThread(() -> Toast.makeText(NoteDetailActivity.this, "Comment failed: " + throwable.getMessage(), Toast.LENGTH_SHORT).show());
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
        // If count is 0 and children > 1, initial collapsed state shows 3 on first tap.
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
                    Toast.makeText(NoteDetailActivity.this, "Please log in before liking comments", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(NoteDetailActivity.this, LoginActivity.class));
                });
            }

            @Override
            public void onError(@NonNull Throwable throwable) {
                runOnUiThread(() -> Toast.makeText(NoteDetailActivity.this, "Operation failed: " + throwable.getMessage(), Toast.LENGTH_SHORT).show());
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

            // Core change:
            // Use FIT_CENTER (full display) instead of CENTER_CROP (fill/crop).
            iv.setScaleType(ImageView.ScaleType.FIT_CENTER);

            // Optional: set a background color so letterboxing looks less abrupt.
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

                // Final fix: restore original click behavior.
                itemView.setOnClickListener(v -> {
                    // Core: invoke the Runnable passed to PhotoPagerAdapter (openFullImage()).
                    PhotoPagerAdapter.this.onClick.run();
                });
            }
        }
    }
}
