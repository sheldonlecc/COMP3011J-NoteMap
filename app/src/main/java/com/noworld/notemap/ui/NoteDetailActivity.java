package com.noworld.notemap.ui;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager2.widget.ViewPager2;
import androidx.recyclerview.widget.RecyclerView;

import com.amap.apis.cluster.demo.RegionItem; // 【重要】导入您的笔记模型
import com.bumptech.glide.Glide; // 导入 Glide
import com.noworld.notemap.R;

import java.util.ArrayList;
import java.util.List;

public class NoteDetailActivity extends AppCompatActivity {

    public static final String EXTRA_NOTE_DATA = "NOTE_DATA"; // Intent Key
    private RegionItem mNote;

    private Toolbar toolbar;
    private ViewPager2 vpDetailPhotos;
    private TextView tvPhotoIndicator;
    private TextView tvDetailTitle;
    private TextView tvDetailDescription;
    private TextView tvDetailType;
    private TextView tvDetailLocation;
    private final List<String> photoUrls = new ArrayList<>();
    private int currentIndex = 0;

    // (未来还可以添加作者信息)

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

        // 3. 填充数据
        if (mNote != null) {
            populateData();
        } else {
            Toast.makeText(this, "加载笔记失败！", Toast.LENGTH_LONG).show();
            finish(); // 如果没有数据，关闭页面
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
    }

    private void initToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("笔记详情");
        }
    }

    private void populateData() {
        // 填充文本
        tvDetailTitle.setText(mNote.getTitle());
        tvDetailDescription.setText(mNote.getDescription());
        tvDetailType.setText("笔记类型: " + mNote.getNoteType());
        tvDetailLocation.setText("拍摄地点: " + mNote.getLocationName());

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
    }

    // 处理 Toolbar 的返回按钮
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish(); // 关闭当前 Activity，返回
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
        public int getItemCount() {
            return data.size();
        }

        static class PhotoVH extends RecyclerView.ViewHolder {
            ImageView iv;

            PhotoVH(@NonNull ImageView itemView, Runnable onClick) {
                super(itemView);
                this.iv = itemView;
                itemView.setOnClickListener(v -> {
                    if (onClick != null) onClick.run();
                });
            }
        }
    }
}
