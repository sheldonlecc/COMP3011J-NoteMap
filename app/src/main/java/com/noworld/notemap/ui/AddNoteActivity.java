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
    private RecyclerView rvImagePreview;
    private RelativeLayout rowNoteType;
    private TextView tvNoteTypeValue;
    private RelativeLayout rowLocation;
    private TextView tvLocationValue;
    private Button btnPublish;

    private ActivityResultLauncher<Intent> pickImageLauncher;
    private ActivityResultLauncher<String> mediaPermissionLauncher;
    private ActivityResultLauncher<Intent> selectLocationLauncher;
    private final List<Uri> selectedImageUris = new ArrayList<>();
    private ImagePreviewAdapter imagePreviewAdapter;
    private double currentLat = 0;
    private double currentLng = 0;

    private GeocodeSearch geocodeSearch;

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
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        noteRepository = AliNoteRepository.getInstance(this);
        tokenStore = TokenStore.getInstance(this);
        userStore = UserStore.getInstance(this);

        // 3. 设置图片预览列表
        rvImagePreview.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        // 【核心修改】这里传入了删除回调
        imagePreviewAdapter = new ImagePreviewAdapter(selectedImageUris, position -> {
            // 当点击删除按钮时触发
            if (position >= 0 && position < selectedImageUris.size()) {
                selectedImageUris.remove(position); // 从数据源移除
                imagePreviewAdapter.notifyItemRemoved(position); // 通知列表刷新
                imagePreviewAdapter.notifyItemRangeChanged(position, selectedImageUris.size()); // 刷新后续位置索引
            }
        });
        rvImagePreview.setAdapter(imagePreviewAdapter);

        // 4. 获取位置
        currentLat = getIntent().getDoubleExtra("CURRENT_LAT", 0);
        currentLng = getIntent().getDoubleExtra("CURRENT_LNG", 0);

        // 5. 初始化功能
        initImagePicker();
        initMediaPermission();
        initLocationPicker();

        try {
            geocodeSearch = new GeocodeSearch(this);
            geocodeSearch.setOnGeocodeSearchListener(this);
        } catch (Exception e) {
            e.printStackTrace();
        }

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

    private void initLocationPicker() {
        selectLocationLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        double lat = result.getData().getDoubleExtra(SelectLocationActivity.EXTRA_LAT, 0);
                        double lng = result.getData().getDoubleExtra(SelectLocationActivity.EXTRA_LNG, 0);
                        String address = result.getData().getStringExtra(SelectLocationActivity.EXTRA_ADDRESS);
                        if (lat != 0 && lng != 0) {
                            currentLat = lat;
                            currentLng = lng;
                            tvLocationValue.setText(address != null ? address : "手动选点");
                        }
                    }
                }
        );
    }

    private void setupClickListeners() {
        ivAddImage.setOnClickListener(v -> ensureMediaPermissionAndPickImage());
        rowNoteType.setOnClickListener(v -> showNoteTypePicker());
        rowLocation.setOnClickListener(v -> showLocationChoiceDialog());
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

    private void showNoteTypePicker() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("选择笔记类型");
        builder.setItems(noteTypes, (dialog, which) -> {
            String selectedType = noteTypes[which].toString();
            tvNoteTypeValue.setText(selectedType);
        });
        builder.create().show();
    }

    private void showLocationChoiceDialog() {
        String[] options = {"使用当前位置", "手动选点"};
        new AlertDialog.Builder(this)
                .setTitle("选择拍摄地点")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        if (currentLat != 0 && currentLng != 0) {
                            tvLocationValue.setText("正在获取地址...");
                            LatLonPoint latLonPoint = new LatLonPoint(currentLat, currentLng);
                            RegeocodeQuery query = new RegeocodeQuery(latLonPoint, 200, GeocodeSearch.AMAP);
                            geocodeSearch.getFromLocationAsyn(query);
                        } else {
                            Toast.makeText(this, "无法获取当前位置, 请返回地图重试", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Intent intent = new Intent(this, SelectLocationActivity.class);
                        intent.putExtra("CURRENT_LAT", currentLat);
                        intent.putExtra("CURRENT_LNG", currentLng);
                        selectLocationLauncher.launch(intent);
                    }
                })
                .show();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
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

    @Override
    public void onRegeocodeSearched(RegeocodeResult result, int rCode) {
        if (rCode == 1000) {
            if (result != null && result.getRegeocodeAddress() != null
                    && result.getRegeocodeAddress().getFormatAddress() != null) {
                String address = result.getRegeocodeAddress().getFormatAddress();
                tvLocationValue.setText(address);
            } else {
                tvLocationValue.setText("未找到地址");
            }
        } else {
            tvLocationValue.setText("获取地址失败: " + rCode);
        }
    }

    @Override
    public void onGeocodeSearched(GeocodeResult geocodeResult, int i) { }

    // ===========================================
    // 【核心修改】更新后的内部类适配器
    // ===========================================
    private static class ImagePreviewAdapter extends RecyclerView.Adapter<ImagePreviewAdapter.ImageVH> {

        private final List<Uri> data;
        // 增加删除回调
        private final OnDeleteListener deleteListener;

        interface OnDeleteListener {
            void onDelete(int position);
        }

        ImagePreviewAdapter(List<Uri> data, OnDeleteListener listener) {
            this.data = data;
            this.deleteListener = listener;
        }

        @NonNull
        @Override
        public ImageVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            // 使用修改后的布局 item_image_preview
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

            // 绑定删除按钮点击事件
            holder.ivDelete.setOnClickListener(v -> {
                if (deleteListener != null) {
                    deleteListener.onDelete(holder.getAdapterPosition());
                }
            });
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        static class ImageVH extends RecyclerView.ViewHolder {
            ImageView iv;
            ImageView ivDelete; // 新增删除按钮

            ImageVH(@NonNull View itemView) {
                super(itemView);
                // 确保这里 ID 和 item_image_preview.xml 里一致
                iv = itemView.findViewById(R.id.iv_image);
                ivDelete = itemView.findViewById(R.id.iv_delete);
            }
        }
    }

    private interface UploadAllCallback {
        void onSuccess(List<String> imageUrls);
        void onError(@NonNull Throwable throwable);
    }
}