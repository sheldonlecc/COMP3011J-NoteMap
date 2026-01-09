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
import com.github.chrisbanes.photoview.PhotoView; // Key import: PhotoView

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;
import com.noworld.notemap.R;

/**
 * Full-screen image viewer with zooming and watermark saving.
 */
public class PictureActivity extends AppCompatActivity {

    public static final String EXTRA_IMAGE_URL = "EXTRA_IMAGE_URL";
    public static final String EXTRA_AUTHOR_NAME = "EXTRA_AUTHOR_NAME";
    private static final int PERMISSION_REQUEST_CODE = 101;

    // Replace ImageView with PhotoView to support pinch-to-zoom.
    private PhotoView iv_picture;
    private String mImageUrl;
    private String mAuthorName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_picture);

        // Read input data.
        mImageUrl = getIntent().getStringExtra(EXTRA_IMAGE_URL);
        mAuthorName = getIntent().getStringExtra(EXTRA_AUTHOR_NAME);

        initView();
        initPicture();

        // Tap to exit.
        iv_picture.setOnClickListener(v -> finish());

        // Long-press to show save confirmation.
        iv_picture.setOnLongClickListener(v -> {
            showSaveConfirmDialog();
            return true;
        });
    }

    private void initView() {
        // Find PhotoView instance.
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
            // Load a default image as fallback.
            Glide.with(this).load(R.drawable.ic_car).into(iv_picture);
        }
    }

    /**
     * Show a confirmation dialog before saving.
     */
    private void showSaveConfirmDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Save image")
                .setMessage("Save this image to your gallery?")
                .setPositiveButton("Save", (dialog, which) -> {
                    requestStoragePermissionAndSaveImage();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Check/request storage permission, then save the image.
     */
    private void requestStoragePermissionAndSaveImage() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            // Request permission.
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST_CODE);
        } else {
            // Permission already granted; save directly.
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
                Toast.makeText(this, "Storage permission is required to save images", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Download the image with Glide before adding a watermark.
     */
    private void downloadImageAndSave() {
        Toast.makeText(this, "Saving image...", Toast.LENGTH_SHORT).show();
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
     * Async task: add watermark and save the image.
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
                Toast.makeText(context, "Image saved to gallery", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(context, "Failed to save image", Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * Save a bitmap to the gallery.
     */
    private static boolean saveBitmapToGallery(Context context, Bitmap bitmap) {
        // Create file path.
        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        if (!storageDir.exists() && !storageDir.mkdirs()) {
            Log.e("PictureActivity", "Unable to create image directory");
            return false;
        }

        String fileName = "NoteMap_" + System.currentTimeMillis() + ".jpg";
        File imageFile = new File(storageDir, fileName);

        try (OutputStream outputStream = new FileOutputStream(imageFile)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream);
            outputStream.flush();

            // Notify MediaStore to refresh.
            Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            mediaScanIntent.setData(Uri.fromFile(imageFile));
            context.sendBroadcast(mediaScanIntent);
            return true;
        } catch (IOException e) {
            Log.e("PictureActivity", "Failed to save image: " + e.getMessage());
            return false;
        }
    }

    /**
     * Add a styled watermark.
     */
    private static Bitmap addWatermark(Bitmap originalBitmap, String rawAuthorName) {
        int width = originalBitmap.getWidth();
        int height = originalBitmap.getHeight();
        Bitmap newBitmap = Bitmap.createBitmap(width, height, originalBitmap.getConfig());
        Canvas canvas = new Canvas(newBitmap);
        canvas.drawBitmap(originalBitmap, 0, 0, null);

        // ------------------ Watermark configuration ------------------
        String authorName = (rawAuthorName != null && !rawAuthorName.isEmpty()) ? rawAuthorName : "Unknown author";
        String watermarkText = "NoteApp By " + authorName;

        // Size and position.
        float extMargin = 40f; // Outer margin.
        float intPadding = 20f; // Inner text padding.
        float cornerRadius = 15f; // Corner radius.

        // Text paint.
        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(40f); // Font size.
        textPaint.setTypeface(Typeface.DEFAULT_BOLD); // Bold.

        // Measure text size.
        Paint.FontMetrics fm = textPaint.getFontMetrics();
        float textWidth = textPaint.measureText(watermarkText);
        float textHeight = fm.bottom - fm.top;

        // Background box size.
        float boxWidth = textWidth + intPadding * 2;
        float boxHeight = textHeight + intPadding * 2;

        // Background box position (bottom-right).
        float boxLeft = width - extMargin - boxWidth;
        float boxTop = height - extMargin - boxHeight;
        float boxRight = width - extMargin;
        float boxBottom = height - extMargin;

        // Draw background box.
        Paint boxPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        boxPaint.setColor(Color.argb(200, 255, 255, 255)); // Semi-transparent white.
        boxPaint.setStyle(Paint.Style.FILL);

        // Draw rounded rectangle.
        RectF rect = new RectF(boxLeft, boxTop, boxRight, boxBottom);
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, boxPaint);

        // Draw text.
        // Vertical centering: use baseline to keep text centered in the box.
        float textX = boxLeft + intPadding;
        float textY = boxTop + intPadding + (textHeight - fm.bottom + fm.top) / 2 - fm.top;

        canvas.drawText(watermarkText, textX, textY, textPaint);

        return newBitmap;
    }
}
