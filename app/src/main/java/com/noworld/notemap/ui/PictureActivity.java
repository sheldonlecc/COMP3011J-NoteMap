package com.noworld.notemap.ui;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.github.chrisbanes.photoview.PhotoView; // 【关键】导入 PhotoView

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;
import com.noworld.notemap.R;

/**
 * 全屏图片查看器，支持图片缩放和水印保存功能。
 */
public class PictureActivity extends AppCompatActivity {

    public static final String EXTRA_IMAGE_URL = "EXTRA_IMAGE_URL";
    public static final String EXTRA_AUTHOR_NAME = "EXTRA_AUTHOR_NAME";
    private static final int PERMISSION_REQUEST_CODE = 101;

    // 【修改】将 ImageView 替换为 PhotoView，实现双指缩放
    private PhotoView iv_picture;
    private String mImageUrl;
    private String mAuthorName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_picture);

        // 获取传递的数据
        mImageUrl = getIntent().getStringExtra(EXTRA_IMAGE_URL);
        mAuthorName = getIntent().getStringExtra(EXTRA_AUTHOR_NAME);

        initView();
        initPicture();

        // 点击退出
        iv_picture.setOnClickListener(v -> finish());

        // 长按监听：弹出确认对话框
        iv_picture.setOnLongClickListener(v -> {
            showSaveConfirmDialog();
            return true;
        });
    }

    private void initView() {
        // 【修改】查找 PhotoView 实例
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
            // 加载默认图片作为错误处理
            Glide.with(this).load(R.drawable.ic_car).into(iv_picture);
        }
    }

    /**
     * 弹窗确认是否保存图片。
     */
    private void showSaveConfirmDialog() {
        new AlertDialog.Builder(this)
                .setTitle("保存图片")
                .setMessage("是否将这张图片保存到相册？")
                .setPositiveButton("保存", (dialog, which) -> {
                    requestStoragePermissionAndSaveImage();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    /**
     * 检查并请求存储权限，然后开始保存图片。
     */
    private void requestStoragePermissionAndSaveImage() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            // 请求权限
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST_CODE);
        } else {
            // 已有权限，直接保存
            downloadImageAndSave();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                downloadImageAndSave();
            } else {
                Toast.makeText(this, "需要存储权限才能保存图片", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * 使用 Glide 下载图片并准备添加水印。
     */
    private void downloadImageAndSave() {
        Toast.makeText(this, "正在保存图片...", Toast.LENGTH_SHORT).show();
        Glide.with(this)
                .asBitmap()
                .load(mImageUrl)
                .into(new SimpleTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(Bitmap resource, Transition<? super Bitmap> transition) {
                        new SaveImageTask(PictureActivity.this, mAuthorName).execute(resource);
                    }
                });
    }

    /**
     * 异步任务：添加水印并保存图片。
     */
    private static class SaveImageTask extends AsyncTask<Bitmap, Void, Boolean> {
        private final Context context;
        private final String authorName;

        public SaveImageTask(Context context, String authorName) {
            this.context = context;
            this.authorName = authorName;
        }

        @Override
        protected Boolean doInBackground(Bitmap... bitmaps) {
            if (bitmaps.length == 0 || bitmaps[0] == null) return false;

            Bitmap originalBitmap = bitmaps[0];
            Bitmap watermarkedBitmap = addWatermark(originalBitmap, authorName);
            return saveBitmapToGallery(context, watermarkedBitmap);
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                Toast.makeText(context, "图片已保存到相册", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(context, "图片保存失败", Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * 将 Bitmap 保存到图库 (Gallery)。
     */
    private static boolean saveBitmapToGallery(Context context, Bitmap bitmap) {
        // 创建文件路径
        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        if (!storageDir.exists() && !storageDir.mkdirs()) {
            Log.e("PictureActivity", "无法创建图片目录");
            return false;
        }

        String fileName = "NoteMap_" + System.currentTimeMillis() + ".jpg";
        File imageFile = new File(storageDir, fileName);

        try (OutputStream outputStream = new FileOutputStream(imageFile)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream);
            outputStream.flush();

            // 通知媒体库更新
            Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            mediaScanIntent.setData(Uri.fromFile(imageFile));
            context.sendBroadcast(mediaScanIntent);
            return true;
        } catch (IOException e) {
            Log.e("PictureActivity", "保存图片失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 添加美观的水印。
     */
    private static Bitmap addWatermark(Bitmap originalBitmap, String rawAuthorName) {
        int width = originalBitmap.getWidth();
        int height = originalBitmap.getHeight();
        Bitmap newBitmap = Bitmap.createBitmap(width, height, originalBitmap.getConfig());
        Canvas canvas = new Canvas(newBitmap);
        canvas.drawBitmap(originalBitmap, 0, 0, null);

        // ------------------ 水印配置 ------------------
        String authorName = (rawAuthorName != null && !rawAuthorName.isEmpty()) ? rawAuthorName : "未知作者";
        String watermarkText = "NoteApp By " + authorName;

        // 尺寸和位置
        float extMargin = 40f; // 外部边距
        float intPadding = 20f; // 内部文字边距
        float cornerRadius = 15f; // 圆角半径

        // 文本画笔
        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(40f); // 字体大小
        textPaint.setTypeface(Typeface.DEFAULT_BOLD); // 粗体

        // 测量文本大小
        Paint.FontMetrics fm = textPaint.getFontMetrics();
        float textWidth = textPaint.measureText(watermarkText);
        float textHeight = fm.bottom - fm.top;

        // 背景框尺寸
        float boxWidth = textWidth + intPadding * 2;
        float boxHeight = textHeight + intPadding * 2;

        // 背景框位置（右下角定位）
        float boxLeft = width - extMargin - boxWidth;
        float boxTop = height - extMargin - boxHeight;
        float boxRight = width - extMargin;
        float boxBottom = height - extMargin;

        // 绘制背景框
        Paint boxPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        boxPaint.setColor(Color.argb(200, 255, 255, 255)); // 半透明白色
        boxPaint.setStyle(Paint.Style.FILL);

        // 绘制圆角矩形
        RectF rect = new RectF(boxLeft, boxTop, boxRight, boxBottom);
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, boxPaint);

        // 绘制文本
        // 垂直居中修正：使用 baseline 确保文字在 box 中居中
        float textX = boxLeft + intPadding;
        float textY = boxTop + intPadding + (textHeight - fm.bottom + fm.top) / 2 - fm.top;

        canvas.drawText(watermarkText, textX, textY, textPaint);

        return newBitmap;
    }
}