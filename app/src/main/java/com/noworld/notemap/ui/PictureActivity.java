package com.noworld.notemap.ui;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.activity.EdgeToEdge;

import com.bumptech.glide.Glide;
import com.noworld.notemap.R;

// 确保您的代码中包含了所有必要的 import 语句，特别是 Bitmap, Canvas, Paint, MediaStore 等。

public class PictureActivity extends AppCompatActivity {

    public static final String EXTRA_IMAGE_URL = "EXTRA_IMAGE_URL";
    public static final String EXTRA_AUTHOR_NAME = "EXTRA_AUTHOR_NAME"; // 【新增】用于接收作者名

    private static final int REQUEST_WRITE_STORAGE = 1; // 权限请求码

    private ImageView iv_picture;
    private String mImageUrl;
    private String mAuthorName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_picture);

        // 【新增】获取作者名
        mImageUrl = getIntent().getStringExtra(EXTRA_IMAGE_URL);
        mAuthorName = getIntent().getStringExtra(EXTRA_AUTHOR_NAME);

        initView();
        initPicture();

        // 点击退出
        iv_picture.setOnClickListener(v -> finish());

        // 【核心新增】长按监听器
        iv_picture.setOnLongClickListener(v -> {
            showSaveConfirmDialog();
            return true; // 消费掉长按事件
        });
    }

    private void initView() {
        iv_picture = findViewById(R.id.iv_picture);
    }

    private void initPicture() {
        if (mImageUrl != null && !mImageUrl.isEmpty()) {
            Glide.with(this)
                    .load(mImageUrl)
                    .placeholder(R.drawable.ic_car)
                    .error(R.drawable.ic_car)
                    .into(iv_picture);
        } else {
            Glide.with(this).load(R.drawable.ic_car).into(iv_picture);
        }
    }

    // ===========================================
    // 【新增功能】保存图片逻辑和水印
    // ===========================================

    // 【新增方法】显示确认保存对话框
    private void showSaveConfirmDialog() {
        new AlertDialog.Builder(this)
                .setTitle("保存图片")
                .setMessage("是否将这张图片保存到相册？")
                .setPositiveButton("确定", (dialog, which) -> requestStoragePermissionAndSaveImage(mImageUrl))
                .setNegativeButton("取消", null)
                .show();
    }

    // 【新增方法】请求存储权限并保存图片
    private void requestStoragePermissionAndSaveImage(String imageUrl) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            // 如果没有权限，请求权限
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_WRITE_STORAGE);
        } else {
            // 已经有权限，直接保存图片
            saveImageWithWatermark(imageUrl);
        }
    }

    // 【新增方法】处理权限请求结果
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_WRITE_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 权限已授予，再次尝试保存图片
                saveImageWithWatermark(mImageUrl);
            } else {
                // 权限被拒绝
                Toast.makeText(this, "保存图片需要存储权限", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // 【新增方法】保存图片 (包含水印逻辑)
    private void saveImageWithWatermark(String imageUrl) {
        new Thread(() -> {
            try {
                // 1. 下载原始图片
                Bitmap originalBitmap = Glide.with(PictureActivity.this)
                        .asBitmap()
                        .load(imageUrl)
                        .submit()
                        .get(); // 阻塞调用，必须在子线程

                if (originalBitmap == null) {
                    runOnUiThread(() -> Toast.makeText(PictureActivity.this, "图片下载失败", Toast.LENGTH_SHORT).show());
                    return;
                }

                // 2. 添加水印
                String authorName = mAuthorName != null ? mAuthorName : "未知作者";
                String watermarkText = "NoteApp By " + authorName;
                Bitmap watermarkedBitmap = addWatermark(originalBitmap, watermarkText);

                // 3. 保存带水印的图片到相册
                String displayName = "NoteApp_Image_" + System.currentTimeMillis() + ".jpg";
                saveBitmapToGallery(watermarkedBitmap, displayName);

                runOnUiThread(() -> Toast.makeText(PictureActivity.this, "图片已保存到相册", Toast.LENGTH_SHORT).show());

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(PictureActivity.this, "保存图片失败: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    // 【优化后的新增方法】添加美观水印 (已优化边距、圆角和文字居中)
    private Bitmap addWatermark(Bitmap originalBitmap, String watermarkText) {
        Bitmap mutableBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(mutableBitmap);

        // --- 样式定义 ---
        int extMargin = 40;     // 外部边距：水印框距离图片边缘的距离
        int intPadding = 20;    // 内部填充：文字距离水印框内部边缘的距离
        float cornerRadius = 15f; // 圆角半径

        // 设置水印文本画笔
        Paint textPaint = new Paint();
        textPaint.setColor(Color.BLACK); // 字体颜色
        textPaint.setTextSize(40); // 字体大小增大
        textPaint.setFakeBoldText(true); // 加粗
        textPaint.setAntiAlias(true);

        // 计算文字宽度和高度
        float textWidth = textPaint.measureText(watermarkText);
        Paint.FontMetrics fm = textPaint.getFontMetrics();
        float textTotalHeight = fm.descent - fm.ascent;

        // 设置水印背景画笔 (半透明白底)
        Paint backgroundPaint = new Paint();
        backgroundPaint.setColor(Color.argb(200, 255, 255, 255)); // 80% 不透明的白色
        backgroundPaint.setStyle(Paint.Style.FILL);

        // --- 矩形位置计算 (右下角) ---

        // 水印框的宽度和高度
        float rectWidth = textWidth + 2 * intPadding;
        float rectHeight = textTotalHeight + 2 * intPadding;

        // 矩形坐标
        float rectRight = mutableBitmap.getWidth() - extMargin;
        float rectBottom = mutableBitmap.getHeight() - extMargin;
        float rectLeft = rectRight - rectWidth;
        float rectTop = rectBottom - rectHeight;

        // 绘制圆角矩形背景
        canvas.drawRoundRect(rectLeft, rectTop, rectRight, rectBottom, cornerRadius, cornerRadius, backgroundPaint);

        // 绘制水印文字
        // 计算文本的 X 轴位置 (在矩形内居中)
        float textX = rectLeft + intPadding;

        // 计算文本的 Y 轴位置 (在矩形内垂直居中)
        // 垂直居中公式: rectTop + (rectHeight / 2) - ((fm.ascent + fm.descent) / 2)
        float textY = rectTop + (rectHeight / 2) - ((fm.ascent + fm.descent) / 2);

        canvas.drawText(watermarkText, textX, textY, textPaint);

        return mutableBitmap;
    }

    // 【新增方法】保存 Bitmap 到相册
    private void saveBitmapToGallery(Bitmap bitmap, String displayName) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, displayName);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        values.put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/NoteApp");
            values.put(MediaStore.Images.Media.IS_PENDING, 1); // 标记为待处理
        }

        ContentResolver resolver = getContentResolver();
        android.net.Uri uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        if (uri != null) {
            try (java.io.OutputStream fos = resolver.openOutputStream(uri)) {
                if (fos != null) {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos); // 压缩图片
                    fos.flush();
                }
            } catch (java.io.IOException e) {
                e.printStackTrace();
            } finally {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    values.clear();
                    values.put(MediaStore.Images.Media.IS_PENDING, 0); // 取消待处理标记
                    resolver.update(uri, values, null, null);
                }
            }
        }
    }
}