package com.noworld.notemap.ui;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
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

import com.amap.apis.cluster.demo.RegionItem;
import com.bumptech.glide.Glide;
import com.noworld.notemap.R;
import com.noworld.notemap.data.AliNoteRepository;
import com.noworld.notemap.data.LikedStore;
import com.noworld.notemap.data.TokenStore;
import com.noworld.notemap.data.UserStore;
import com.noworld.notemap.data.model.CommentItem;

import java.util.ArrayList;
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

    private final List<CommentItem> comments = new ArrayList<>();
    private CommentAdapter commentAdapter;

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
            tvInputComment.setOnClickListener(v -> showCommentDialog());
        }
        if (layoutLike != null) {
            layoutLike.setOnClickListener(v -> handleLikeClick());
        }
        if (layoutComment != null) {
            layoutComment.setOnClickListener(v -> showCommentDialog());
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
                    comments.clear();
                    if (list != null) {
                        comments.addAll(list);
                    }
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
        int count = comments.size();
        if (tvCommentCount != null) {
            tvCommentCount.setText(String.valueOf(count));
        }
        if (tvCommentSectionTitle != null) {
            tvCommentSectionTitle.setText("共 " + count + " 条评论");
        }
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

    private void showCommentDialog() {
        if (mNote == null) return;
        if (!ensureLoggedInForComment()) return;

        final EditText input = new EditText(this);
        input.setHint("友善评论，理性发言");
        int padding = (int) (getResources().getDisplayMetrics().density * 12);
        input.setPadding(padding, padding / 2, padding, padding / 2);
        input.setMinLines(1);
        input.setMaxLines(4);

        new AlertDialog.Builder(this)
                .setTitle("发表评论")
                .setView(input)
                .setPositiveButton("发送", (dialog, which) -> {
                    String content = input.getText().toString().trim();
                    if (TextUtils.isEmpty(content)) {
                        Toast.makeText(NoteDetailActivity.this, "评论内容不能为空", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    submitComment(content);
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void submitComment(String content) {
        if (mNote == null) return;
        noteRepository.addComment(mNote.getNoteId(), content, new AliNoteRepository.AddCommentCallback() {
            @Override
            public void onSuccess(CommentItem newComment) {
                runOnUiThread(() -> {
                    if (commentAdapter != null) {
                        commentAdapter.addCommentToTop(newComment);
                    } else {
                        comments.add(0, newComment);
                    }
                    updateCommentCounter();
                    if (rvComments != null) {
                        rvComments.scrollToPosition(0);
                    }
                    Toast.makeText(NoteDetailActivity.this, "评论成功", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onRequireLogin() {
                runOnUiThread(() -> {
                    Toast.makeText(NoteDetailActivity.this, "登录已过期，请重新登录", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(NoteDetailActivity.this, LoginActivity.class));
                });
            }

            @Override
            public void onError(@NonNull Throwable throwable) {
                runOnUiThread(() -> Toast.makeText(NoteDetailActivity.this, "评论失败: " + throwable.getMessage(), Toast.LENGTH_SHORT).show());
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
