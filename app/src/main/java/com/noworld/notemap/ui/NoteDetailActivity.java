package com.noworld.notemap.ui;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
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

    private LinearLayout layoutLike;
    private ImageView ivDetailLike;
    private TextView tvLikeCount;
    private TextView tvCommentCount;

    private AliNoteRepository noteRepository;
    private LikedStore likedStore;
    private UserStore userStore;
    private TokenStore tokenStore;

    private final List<String> photoUrls = new ArrayList<>();
    private int currentIndex = 0;

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

        // 【请临时添加这一行代码来调试】
        Toast.makeText(this, "我是作者吗? " + isAuthor +
                " (我的ID: " + currentUid + " 笔记ID: " + mNote.getAuthorId() + ")", Toast.LENGTH_LONG).show();
        // 【调试完成后请删除】

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

        layoutLike = findViewById(R.id.layout_like);
        ivDetailLike = findViewById(R.id.iv_detail_like);
        tvLikeCount = findViewById(R.id.tv_detail_like_count);
        tvCommentCount = findViewById(R.id.tv_detail_comment_count);

        if (rvComments != null) {
            rvComments.setLayoutManager(new LinearLayoutManager(this));
            rvComments.setNestedScrollingEnabled(false);
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
            tvInputComment.setOnClickListener(v -> {
                Toast.makeText(this, "点击了评论框", Toast.LENGTH_SHORT).show();
            });
        }
        if (layoutLike != null) {
            layoutLike.setOnClickListener(v -> handleLikeClick());
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
            tvDetailTime.setText("发布于 " + (time != null ? time : "2023-11-27"));
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
        List<CommentItem> comments = new ArrayList<>();
        comments.add(new CommentItem("爱吃猫的鱼", "这个地方真不错！我也想去。", "10分钟前"));

        CommentAdapter adapter = new CommentAdapter(comments);
        if (rvComments != null) {
            rvComments.setAdapter(adapter);
        }

        if (tvCommentCount != null) {
            tvCommentCount.setText(String.valueOf(comments.size()));
        }

        TextView tvSectionTitle = findViewById(R.id.tv_comment_section_title);
        if (tvSectionTitle != null) {
            tvSectionTitle.setText("共 " + comments.size() + " 条评论");
        }
    }

    private void openFullImage() {
        String photoUrl = photoUrls.get(currentIndex);
        if (photoUrl == null || photoUrl.isEmpty()) return;
        Intent intent = new Intent(this, PictureActivity.class);
        intent.putExtra(PictureActivity.EXTRA_IMAGE_URL, photoUrl);
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

    private static class PhotoPagerAdapter extends RecyclerView.Adapter<PhotoPagerAdapter.PhotoVH> {
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
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            return new PhotoVH(iv, onClick);
        }

        @Override
        public void onBindViewHolder(@NonNull PhotoVH holder, int position) {
            String url = data.get(position);
            Glide.with(holder.iv.getContext())
                    .load(url)
                    .placeholder(R.drawable.ic_car)
                    .error(R.drawable.ic_car)
                    .into(holder.iv);
        }

        @Override
        public int getItemCount() { return data.size(); }

        static class PhotoVH extends RecyclerView.ViewHolder {
            ImageView iv;
            PhotoVH(@NonNull ImageView itemView, Runnable onClick) {
                super(itemView);
                this.iv = itemView;
                itemView.setOnClickListener(v -> { if (onClick != null) onClick.run(); });
            }
        }
    }
}