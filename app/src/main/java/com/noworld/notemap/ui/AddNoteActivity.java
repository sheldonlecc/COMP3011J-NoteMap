package com.noworld.notemap.ui;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.geocoder.GeocodeResult;
import com.amap.api.services.geocoder.GeocodeSearch;
import com.amap.api.services.geocoder.RegeocodeQuery;
import com.amap.api.services.geocoder.RegeocodeResult;
import com.google.android.material.textfield.TextInputEditText;
import com.noworld.notemap.R;
import com.noworld.notemap.data.AliNoteRepository;
import com.noworld.notemap.data.MapNote;
import com.noworld.notemap.data.TokenStore;
import com.noworld.notemap.data.UserStore;
import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

public class AddNoteActivity extends AppCompatActivity implements GeocodeSearch.OnGeocodeSearchListener {

    private static final int MAX_IMAGES = 9;

    private Toolbar toolbar;
    private TextInputEditText etTitle;
    private TextInputEditText etStory;
    private ImageView ivAddImage;
    private androidx.recyclerview.widget.RecyclerView rvImagePreview;
    private RelativeLayout rowNoteType;
    private TextView tvNoteTypeValue;
    private RelativeLayout rowLocation;
    private TextView tvLocationValue;
    private Button btnPublish;

    private ActivityResultLauncher<Intent> pickImageLauncher;
    private ActivityResultLauncher<String> mediaPermissionLauncher;
    private final List<Uri> selectedImageUris = new ArrayList<>();
    private ImagePreviewAdapter imagePreviewAdapter;
    private double currentLat = 0;
    private double currentLng = 0;

    // [新增] 逆地理编码查询器
    private GeocodeSearch geocodeSearch;

    // 笔记类型选项
    private final CharSequence[] noteTypes = {
            "种草", "攻略", "测评", "分享", "合集", "教程", "开箱", "Vlog", "探店"
    };

    private AliNoteRepository noteRepository;
    private TokenStore tokenStore;
    private UserStore userStore;
    private boolean isPublishing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_note);

        // 1. 绑定控件
        toolbar = findViewById(R.id.toolbar_add_note);
        etTitle = findViewById(R.id.et_title);
        etStory = findViewById(R.id.et_story);
        ivAddImage = findViewById(R.id.iv_add_image);
        rvImagePreview = findViewById(R.id.rv_image_preview);
        rowNoteType = findViewById(R.id.row_note_type);
        tvNoteTypeValue = findViewById(R.id.tv_note_type_value);
        rowLocation = findViewById(R.id.row_location);
        tvLocationValue = findViewById(R.id.tv_location_value);
        btnPublish = findViewById(R.id.btn_publish);

        // 2. 设置顶部工具栏
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true); // 显示返回箭头
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        noteRepository = AliNoteRepository.getInstance(this);
        tokenStore = TokenStore.getInstance(this);
        userStore = UserStore.getInstance(this);

        // 图片预览列表
        rvImagePreview.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        imagePreviewAdapter = new ImagePreviewAdapter(selectedImageUris);
        rvImagePreview.setAdapter(imagePreviewAdapter);

        // 3. 获取从 MainActivity 传来的当前位置
        currentLat = getIntent().getDoubleExtra("CURRENT_LAT", 0);
        currentLng = getIntent().getDoubleExtra("CURRENT_LNG", 0);


        // 4. 初始化图片选择器
        initImagePicker();
        initMediaPermission();

        // 5. [新增] 初始化地理编码查询器
        try {
            geocodeSearch = new GeocodeSearch(this);
            geocodeSearch.setOnGeocodeSearchListener(this);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 6. 设置点击事件
        setupClickListeners();
    }

    private void initImagePicker() {
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        if (result.getData().getClipData() != null) {
                            int count = result.getData().getClipData().getItemCount();
                            for (int i = 0; i < count; i++) {
                                Uri uri = result.getData().getClipData().getItemAt(i).getUri();
                                if (uri != null && selectedImageUris.size() < MAX_IMAGES) {
                                    selectedImageUris.add(uri);
                                }
                            }
                        } else if (result.getData().getData() != null) {
                            if (selectedImageUris.size() < MAX_IMAGES) {
                                selectedImageUris.add(result.getData().getData());
                            }
                        }
                        if (selectedImageUris.size() >= MAX_IMAGES) {
                            Toast.makeText(this, "最多选择9张图片", Toast.LENGTH_SHORT).show();
                        }
                        imagePreviewAdapter.notifyDataSetChanged();
                    }
                });
    }

    private void initMediaPermission() {
        mediaPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (granted) {
                        openGallery();
                    } else {
                        Toast.makeText(this, "需要相册读取权限以选择图片", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setupClickListeners() {
        // 点击方框 (+) 添加图片
        ivAddImage.setOnClickListener(v -> {
            ensureMediaPermissionAndPickImage();
        });

        // 点击 "笔记类型" 行
        rowNoteType.setOnClickListener(v -> {
            showNoteTypePicker();
        });

        // [修改] 点击 "拍摄地点" 行
        rowLocation.setOnClickListener(v -> {
            if (currentLat != 0 && currentLng != 0) {
                // 开始逆地理编码查询
                tvLocationValue.setText("正在获取地址...");
                // 第一个参数表示一个LatLonPoint对象，第二参数表示范围多少米，第三个参数表示是火星坐标系还是GPS原生坐标系
                LatLonPoint latLonPoint = new LatLonPoint(currentLat, currentLng);
                RegeocodeQuery query = new RegeocodeQuery(latLonPoint, 200, GeocodeSearch.AMAP);
                geocodeSearch.getFromLocationAsyn(query);
            } else {
                Toast.makeText(this, "无法获取当前位置, 请返回地图重试", Toast.LENGTH_SHORT).show();
            }
        });

        // 点击 "发布" 按钮
        btnPublish.setOnClickListener(v -> {
            if (!isPublishing) {
                publishNote();
            }
        });
    }

    private void ensureMediaPermissionAndPickImage() {
        String permission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ? android.Manifest.permission.READ_MEDIA_IMAGES
                : android.Manifest.permission.READ_EXTERNAL_STORAGE;
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            openGallery();
        } else {
            mediaPermissionLauncher.launch(permission);
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        pickImageLauncher.launch(intent);
    }

    private void uploadAllImages(List<Uri> uris, UploadAllCallback callback) {
        if (uris == null || uris.isEmpty()) {
            callback.onError(new IllegalArgumentException("没有选择图片"));
            return;
        }
        List<String> uploaded = new ArrayList<>();
        uploadNext(uris, 0, uploaded, callback);
    }

    private void uploadNext(List<Uri> uris, int index, List<String> uploaded, UploadAllCallback callback) {
        if (index >= uris.size()) {
            callback.onSuccess(uploaded);
            return;
        }
        Uri uri = uris.get(index);
        noteRepository.uploadImage(uri, new AliNoteRepository.UploadCallback() {
            @Override
            public void onSuccess(String fileUrl) {
                uploaded.add(fileUrl);
                uploadNext(uris, index + 1, uploaded, callback);
            }

            @Override
            public void onError(@NonNull Throwable throwable) {
                callback.onError(throwable);
            }
        });
    }

    /**
     * 显示笔记类型的滑动选择栏 (用简单的对话框实现)
     */
    private void showNoteTypePicker() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("选择笔记类型");
        builder.setItems(noteTypes, (dialog, which) -> {
            // "which" 对应的就是选项的索引
            String selectedType = noteTypes[which].toString();
            tvNoteTypeValue.setText(selectedType);
        });
        builder.create().show();
    }

    /**
     * 处理顶部工具栏的返回按钮点击
     */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish(); // 关闭当前 Activity，返回到 MainActivity
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void publishNote() {
        String title = etTitle.getText() != null ? etTitle.getText().toString().trim() : "";
        String story = etStory.getText() != null ? etStory.getText().toString().trim() : "";
        String noteType = tvNoteTypeValue.getText() != null ? tvNoteTypeValue.getText().toString().trim() : "";
        String locationName = tvLocationValue.getText() != null ? tvLocationValue.getText().toString().trim() : "";

        if (TextUtils.isEmpty(title)) {
            Toast.makeText(this, "请输入标题", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(story)) {
            Toast.makeText(this, "请输入正文", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(noteType)) {
            Toast.makeText(this, "请选择笔记类型", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedImageUris.isEmpty()) {
            Toast.makeText(this, "请选择至少一张图片", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedImageUris.size() > MAX_IMAGES) {
            Toast.makeText(this, "最多可上传9张图片", Toast.LENGTH_SHORT).show();
            return;
        }
        if (currentLat == 0 || currentLng == 0) {
            Toast.makeText(this, "无法获取定位，请返回地图重试", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(tokenStore.getToken())) {
            Toast.makeText(this, "请先登录再发布笔记", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            return;
        }

        final String locationNameFinal = TextUtils.isEmpty(locationName) ? "未知地点" : locationName;
        setPublishing(true);
        uploadAllImages(selectedImageUris, new UploadAllCallback() {
            @Override
            public void onSuccess(List<String> imageUrls) {
                String authorId = userStore.ensureUid(userStore.getUid());
                String authorName = TextUtils.isEmpty(userStore.getUsername()) ? "地图用户" : userStore.getUsername();
                String avatarUrl = userStore.getAvatarUrl();
                MapNote note = new MapNote(
                        title,
                        story,
                        noteType,
                        currentLat,
                        currentLng,
                        locationNameFinal,
                        imageUrls,
                        authorId,
                        authorName,
                        avatarUrl
                );
                noteRepository.publishNote(note, new AliNoteRepository.PublishCallback() {
                    @Override
                    public void onSuccess() {
                        runOnUiThread(() -> {
                            Toast.makeText(AddNoteActivity.this, "发布成功！", Toast.LENGTH_SHORT).show();
                            finish();
                        });
                    }

                    @Override
                    public void onError(@NonNull Throwable throwable) {
                        runOnUiThread(() -> {
                            Toast.makeText(AddNoteActivity.this, "发布失败：" + throwable.getMessage(), Toast.LENGTH_LONG).show();
                            setPublishing(false);
                        });
                    }
                });
            }

            @Override
            public void onError(@NonNull Throwable throwable) {
                runOnUiThread(() -> {
                    Toast.makeText(AddNoteActivity.this, "图片上传失败：" + throwable.getMessage(), Toast.LENGTH_LONG).show();
                    setPublishing(false);
                });
            }
        });
    }

    private void setPublishing(boolean publishing) {
        isPublishing = publishing;
        btnPublish.setEnabled(!publishing);
        btnPublish.setText(publishing ? "发布中..." : "发布");
    }

    // [新增] 逆地理编码的回调方法
    @Override
    public void onRegeocodeSearched(RegeocodeResult result, int rCode) {
        if (rCode == 1000) { // 1000 代表成功
            if (result != null && result.getRegeocodeAddress() != null
                    && result.getRegeocodeAddress().getFormatAddress() != null) {

                String address = result.getRegeocodeAddress().getFormatAddress(); // 这就是 "北京市朝阳区..."
                tvLocationValue.setText(address);
            } else {
                tvLocationValue.setText("未找到地址");
            }
        } else {
            tvLocationValue.setText("获取地址失败: " + rCode);
        }
    }

    // [新增] 地理编码的回调方法 (我们用不到，但必须实现)
    @Override
    public void onGeocodeSearched(GeocodeResult geocodeResult, int i) {
        // 正向地理编码（地址转坐标）的回调，我们用不到
    }

    private static class ImagePreviewAdapter extends RecyclerView.Adapter<ImagePreviewAdapter.ImageVH> {

        private final List<Uri> data;

        ImagePreviewAdapter(List<Uri> data) {
            this.data = data;
        }

        @NonNull
        @Override
        public ImageVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_image_preview, parent, false);
            return new ImageVH(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ImageVH holder, int position) {
            Uri uri = data.get(position);
            Glide.with(holder.iv.getContext())
                    .load(uri)
                    .centerCrop()
                    .into(holder.iv);
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        static class ImageVH extends RecyclerView.ViewHolder {
            ImageView iv;

            ImageVH(@NonNull View itemView) {
                super(itemView);
                iv = itemView.findViewById(R.id.iv_preview);
            }
        }
    }

    private interface UploadAllCallback {
        void onSuccess(List<String> imageUrls);

        void onError(@NonNull Throwable throwable);
    }
}
