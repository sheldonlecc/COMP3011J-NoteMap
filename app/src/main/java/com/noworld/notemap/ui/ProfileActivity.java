package com.noworld.notemap.ui;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
// [删除] import android.widget.Button; // 不再需要 Button
import android.widget.FrameLayout; // [新增]
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast; // [删除] 不再需要 Toast

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

// [新增] 导入 TabLayout
import com.google.android.material.tabs.TabLayout;
import com.noworld.notemap.R;

/**
 * 个人主页 Activity
 */
public class ProfileActivity extends AppCompatActivity {

    private Toolbar toolbar;
    // [修改] 移除旧控件，添加新控件
    private ImageView ivAvatar;
    private TextView tvUsername;
    private TextView tvAccountId; // [新增] 帐号
    private TextView tvSignature; // [新增] 签名

    // [新增] 标签页和内容区域
    private TabLayout tabLayout;
    private FrameLayout contentContainer;
    private TextView tvWorksListPlaceholder;
    private TextView tvLikesListPlaceholder;

    // [删除] private Button btnLogout;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 1. 设置布局文件
        setContentView(R.layout.activity_profile);

        // 2. 绑定控件 (已更新为新 ID)
        toolbar = findViewById(R.id.toolbar_profile);
        ivAvatar = findViewById(R.id.iv_avatar);
        tvUsername = findViewById(R.id.tv_username);
        tvAccountId = findViewById(R.id.tv_account_id); // [新增]
        tvSignature = findViewById(R.id.tv_signature); // [新增]
        tabLayout = findViewById(R.id.tab_layout); // [新增]
        contentContainer = findViewById(R.id.content_container); // [新增]
        tvWorksListPlaceholder = findViewById(R.id.tv_works_list_placeholder); // [新增]
        tvLikesListPlaceholder = findViewById(R.id.tv_likes_list_placeholder); // [新增]
        // [删除] btnLogout = findViewById(R.id.btn_logout);

        // 3. 设置顶部工具栏和返回按钮 (保持不变)
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true); // 显示返回箭头
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        // 4. [删除] 旧的退出登录按钮逻辑
        // btnLogout.setOnClickListener(v -> { ... });

        // 5. [新增] 设置 TabLayout 的点击监听
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                // 当标签被选中时
                if (tab.getPosition() == 0) {
                    // 选中了 "作品"
                    tvWorksListPlaceholder.setVisibility(View.VISIBLE);
                    tvLikesListPlaceholder.setVisibility(View.GONE);
                } else {
                    // 选中了 "点赞"
                    tvWorksListPlaceholder.setVisibility(View.GONE);
                    tvLikesListPlaceholder.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                // 当标签从选中变为未选中时 (我们暂时不需要)
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                // 当标签被再次选中时 (我们暂时不需要)
            }
        });
    }

    /**
     * 处理顶部工具栏的返回按钮点击 (保持不变)
     */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        // android.R.id.home 是 ActionBar 的 "Home" (返回) 按钮的 ID
        if (item.getItemId() == android.R.id.home) {
            finish(); // 关闭当前 Activity，返回到 MainActivity
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}

