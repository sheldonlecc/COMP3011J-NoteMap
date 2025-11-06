package com.noworld.notemap.ui;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log; // [新增] 导入 Log
import android.view.MenuItem;
import android.view.View;
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

// [新增] 导入逆地理编码相关的包
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.geocoder.GeocodeResult;
import com.amap.api.services.geocoder.GeocodeSearch;
import com.amap.api.services.geocoder.RegeocodeQuery;
import com.amap.api.services.geocoder.RegeocodeResult; // [新增] 导入 RegeocodeResult

import com.google.android.material.textfield.TextInputEditText;
import com.noworld.notemap.R;

import android.widget.Toast;

// [修改] 实现 OnGeocodeSearchListener 接口
public class AddNoteActivity extends AppCompatActivity implements GeocodeSearch.OnGeocodeSearchListener {

    private Toolbar toolbar;
    private TextInputEditText etTitle;
    private TextInputEditText etStory;
    private ImageView ivAddImage;
    private RelativeLayout rowNoteType;
    private TextView tvNoteTypeValue;
    private RelativeLayout rowLocation;
    private TextView tvLocationValue;
    private Button btnPublish;

    private ActivityResultLauncher<Intent> pickImageLauncher;
    private Uri selectedImageUri = null;
    private double currentLat = 0;
    private double currentLng = 0;

    // [新增] 逆地理编码查询器
    private GeocodeSearch geocodeSearch;

    // 笔记类型选项
    private final CharSequence[] noteTypes = {
            "种草", "攻略", "测评", "分享", "合集", "教程", "开箱", "Vlog", "探店"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_note);

        // 1. 绑定控件
        toolbar = findViewById(R.id.toolbar_add_note);
        etTitle = findViewById(R.id.et_title);
        etStory = findViewById(R.id.et_story);
        ivAddImage = findViewById(R.id.iv_add_image);
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

        // 3. 获取从 MainActivity 传来的当前位置
        currentLat = getIntent().getDoubleExtra("CURRENT_LAT", 0);
        currentLng = getIntent().getDoubleExtra("CURRENT_LNG", 0);


        // 4. 初始化图片选择器
        initImagePicker();

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
                        selectedImageUri = result.getData().getData();
                        // 将图片显示到 ImageView
                        ivAddImage.setImageURI(selectedImageUri);
                        ivAddImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    }
                });
    }

    private void setupClickListeners() {
        // 点击方框 (+) 添加图片
        ivAddImage.setOnClickListener(v -> {
            // 启动相册
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            pickImageLauncher.launch(intent);
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
            // 提示：这是后端同学实现 P1 [cite:"uploaded:MapNotes_Project_Outline(1).docx"] 功能的地方
            // (他们需要获取 etTitle.getText(), etStory.getText(), selectedImageUri, tvNoteTypeValue.getText(), currentLat, currentLng)
            // 并且 tvLocationValue.getText() 现在是中文地址了
            Toast.makeText(this, "点击了 [发布] (UI占位符)", Toast.LENGTH_SHORT).show();
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
}

