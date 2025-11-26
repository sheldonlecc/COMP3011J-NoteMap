package com.noworld.notemap.ui;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.amap.apis.cluster.demo.RegionItem; // 【重要】导入您的笔记模型
import com.bumptech.glide.Glide; // 导入 Glide
import com.noworld.notemap.R;

public class NoteDetailActivity extends AppCompatActivity {

    public static final String EXTRA_NOTE_DATA = "NOTE_DATA"; // Intent Key
    private RegionItem mNote;

    private Toolbar toolbar;
    private ImageView ivDetailPhoto;
    private TextView tvDetailTitle;
    private TextView tvDetailDescription;
    private TextView tvDetailType;
    private TextView tvDetailLocation;
    private String photoUrl;

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
        ivDetailPhoto = findViewById(R.id.iv_detail_photo);
        tvDetailTitle = findViewById(R.id.tv_detail_title);
        tvDetailDescription = findViewById(R.id.tv_detail_description);
        tvDetailType = findViewById(R.id.tv_detail_type);
        tvDetailLocation = findViewById(R.id.tv_detail_location);
        ivDetailPhoto.setOnClickListener(v -> openFullImage());
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

        // 使用 Glide 加载图片 (依赖 Member B 提供 URL)
        photoUrl = mNote.getPhotoUrl();
        Glide.with(this)
                .load(photoUrl)
                .placeholder(R.drawable.ic_car) // 占位图
                .error(R.drawable.ic_car)       // 失败图
                .into(ivDetailPhoto);
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
        if (photoUrl == null || photoUrl.isEmpty()) return;
        Intent intent = new Intent(this, PictureActivity.class);
        intent.putExtra(PictureActivity.EXTRA_IMAGE_URL, photoUrl);
        startActivity(intent);
    }
}
