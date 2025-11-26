package com.noworld.notemap.ui;

import android.os.Bundle;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.noworld.notemap.R;
import com.bumptech.glide.Glide;

public class PictureActivity extends AppCompatActivity {

    public static final String EXTRA_IMAGE_URL = "EXTRA_IMAGE_URL";

    private ImageView iv_picture;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_picture);

        initView();

        initPicture();

        iv_picture.setOnClickListener(v -> finish());
    }

    private void initView() {
        iv_picture = findViewById(R.id.iv_picture);
    }

    private void initPicture() {
        String url = getIntent().getStringExtra(EXTRA_IMAGE_URL);
        if (url != null && !url.isEmpty()) {
            Glide.with(this)
                    .load(url)
                    .placeholder(R.drawable.ic_car)
                    .error(R.drawable.ic_car)
                    .into(iv_picture);
        } else {
            Glide.with(this).load(R.drawable.ic_car).into(iv_picture);
        }
    }
}
