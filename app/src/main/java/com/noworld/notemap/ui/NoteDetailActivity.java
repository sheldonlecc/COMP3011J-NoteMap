package com.noworld.notemap.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
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

    // 【新增】点赞相关的控件和仓库
    private LinearLayout layoutLike; // 点赞的点击区域
    private ImageView ivDetailLike;  // 点赞图标
    private TextView tvLikeCount;    // 点赞数字
    private TextView tvCommentCount;

    private AliNoteRepository noteRepository;
    private LikedStore likedStore;
    private UserStore userStore;
    private TokenStore tokenStore;

    private final List<String> photoUrls = new ArrayList<>();
    private int currentIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_detail);

        // 1. 初始化数据仓库
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
            populateData();
        } else {
            Toast.makeText(this, "加载笔记数据失败", Toast.LENGTH_SHORT).show();
        }
    }

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

        // 绑定底部互动栏
        layoutLike = findViewById(R.id.layout_like); // 整个点击区域
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

        // 【新增】点赞点击事件
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
                    .into(ivDetailAvatar); // XML里已经有 CardView 包裹了，这里不需要 circleCrop
        }

        if (tvDetailTime != null) {
            String time = mNote.getCreateTime();
            tvDetailTime.setText("发布于 " + (time != null ? time : "2023-11-27"));
        }

        // 【核心】初始化点赞状态
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

    // 【新增】刷新点赞图标和数字
    private void refreshLikeState() {
        if (mNote == null) return;

        // 1. 检查本地数据库是否已点赞
        String uid = userStore.extractUidFromToken(tokenStore.getToken());
        if (uid == null) uid = userStore.getUid();

        Set<String> likedIds = likedStore.getLikedIds(uid);
        boolean isLiked = likedIds.contains(mNote.getNoteId());
        mNote.setLikedByCurrentUser(isLiked);

        // 2. 检查本地是否有最新的点赞数缓存
        Integer storedCount = likedStore.getLikeCount(mNote.getNoteId());
        if (storedCount != null) {
            mNote.setLikeCount(storedCount);
        }

        // 3. 更新 UI
        tvLikeCount.setText(String.valueOf(mNote.getLikeCount()));
        ivDetailLike.setImageResource(isLiked ? R.drawable.ic_like_filled : R.drawable.ic_like);
    }

    // 【新增】处理点赞逻辑
    private void handleLikeClick() {
        // 1. 检查登录
        if (tokenStore.getToken() == null || tokenStore.getToken().isEmpty()) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            return;
        }

        // 2. 调用后端接口
        noteRepository.toggleLike(mNote, new AliNoteRepository.LikeCallback() {
            @Override
            public void onResult(boolean liked, int likeCount) {
                // 3. 更新内存对象
                mNote.setLikedByCurrentUser(liked);
                mNote.setLikeCount(likeCount);

                // 4. 更新本地存储 (保证返回列表页时状态同步)
                String uid = userStore.extractUidFromToken(tokenStore.getToken());
                likedStore.toggle(uid, mNote.getNoteId(), liked);
                likedStore.saveLikeCount(mNote.getNoteId(), likeCount);

                // 5. 更新 UI
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
        comments.add(new CommentItem("旅行家Bob", "拍得真好，请问是用什么相机拍的？", "2小时前"));

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

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
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