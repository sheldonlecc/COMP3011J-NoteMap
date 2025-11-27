package com.noworld.notemap.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager2.widget.ViewPager2;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.amap.apis.cluster.demo.RegionItem;
import com.bumptech.glide.Glide;
import com.noworld.notemap.R;

import java.util.ArrayList;
import java.util.List;

public class NoteDetailActivity extends AppCompatActivity {

    public static final String EXTRA_NOTE_DATA = "NOTE_DATA"; // Intent Key
    private RegionItem mNote;

    // --- 顶部原有控件 ---
    private Toolbar toolbar;
    private ViewPager2 vpDetailPhotos;
    private TextView tvPhotoIndicator;
    private TextView tvDetailTitle;
    private TextView tvDetailDescription;
    private TextView tvDetailType;
    private TextView tvDetailLocation;

    // --- 作者信息控件 ---
    private ImageView ivDetailAvatar;
    private TextView tvDetailAuthor;

    // --- 底部互动栏和评论区控件 ---
    private TextView tvDetailTime;      // 发布时间
    private RecyclerView rvComments;    // 评论列表
    private TextView tvInputComment;    // 底部输入框
    private TextView tvLikeCount;       // 底部点赞数
    private TextView tvCommentCount;    // 底部评论数

    private final List<String> photoUrls = new ArrayList<>();
    private int currentIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_detail);

        // 1. 接收传递过来的笔记数据
        if (getIntent().hasExtra(EXTRA_NOTE_DATA)) {
            mNote = (RegionItem) getIntent().getSerializableExtra(EXTRA_NOTE_DATA);
        }

        // 2. 初始化 View
        initView();
        initToolbar();
        initEvent();

        // 3. 填充数据
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
    }

    private void populateData() {
        // 1. 填充文本信息
        tvDetailTitle.setText(mNote.getTitle());
        tvDetailDescription.setText(mNote.getDescription());
        tvDetailType.setText(mNote.getNoteType() != null ? "#" + mNote.getNoteType() : "#推荐");
        tvDetailLocation.setText(mNote.getLocationName());

        // 2. 填充作者信息
        if (tvDetailAuthor != null) {
            tvDetailAuthor.setText(mNote.getAuthorName());
        }

        // 【核心修改1】作者头像圆形裁剪
        if (ivDetailAvatar != null) {
            Glide.with(this)
                    .load(mNote.getAuthorAvatarUrl())
                    .placeholder(R.drawable.ic_profile) // 建议换成默认头像图，而不是车
                    .error(R.drawable.ic_profile)
                    .circleCrop() // 【这里已经是圆形了】
                    .into(ivDetailAvatar);
        }

        // 3. 填充时间
        if (tvDetailTime != null) {
            String time = mNote.getCreateTime();
            tvDetailTime.setText("发布于 " + (time != null ? time : "2023-11-27"));
        }

        // 4. 填充数据
        if (tvLikeCount != null) {
            tvLikeCount.setText(String.valueOf(mNote.getLikeCount()));
        }

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

        // 5. 加载评论
        loadComments();
    }

    private void loadComments() {
        List<CommentItem> comments = new ArrayList<>();
        comments.add(new CommentItem("爱吃猫的鱼", "这个地方真不错！我也想去。", "10分钟前"));
        comments.add(new CommentItem("旅行家Bob", "拍得真好，请问是用什么相机拍的？", "2小时前"));
        comments.add(new CommentItem("路人甲", "已收藏，下次去打卡。", "1天前"));
        comments.add(new CommentItem("摄影师David", "构图很棒！", "2天前"));

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

    // --- 内部类：图片轮播 ---
    private static class PhotoPagerAdapter extends RecyclerView.Adapter<PhotoPagerAdapter.PhotoVH> {
        private final List<String> data;
        private final Runnable onClick;

        PhotoPagerAdapter(List<String> data, Runnable onClick) {
            this.data = data;
            this.onClick = onClick;
        }

        @NonNull
        @Override
        public PhotoVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ImageView iv = new ImageView(parent.getContext());
            iv.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
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

    // --- 内部类：评论模型 ---


}