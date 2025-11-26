package com.noworld.notemap.ui;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Build;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.google.android.material.tabs.TabLayout;
import com.noworld.notemap.data.LikedStore;
import com.noworld.notemap.data.dto.LoginResponse;
import com.noworld.notemap.data.TokenStore;
import com.noworld.notemap.data.UserStore;
import com.noworld.notemap.data.AliNoteRepository;
import com.noworld.notemap.data.MapNote;
import com.noworld.notemap.R;
import com.amap.apis.cluster.demo.RegionItem;
import com.noworld.notemap.ui.NoteCardAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 个人主页 Activity
 */
public class ProfileActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private ImageView ivAvatar;
    private TextView tvUsername;
    private TextView tvAccountId;
    private TextView tvSignature;
    private Button btnProfileAction;

    private TabLayout tabLayout;
    private FrameLayout contentContainer;
    private TextView tvEmpty;
    private TextView tvLikesListPlaceholder;
    private RecyclerView rvMyNotes;
    private ProgressBar progressBar;
    private NoteCardAdapter myCardsAdapter;
    private NoteCardAdapter likedCardsAdapter;
    private final List<RegionItem> likedRegionItems = new ArrayList<>();
    private final List<RegionItem> myRegionItems = new ArrayList<>();

    private ActivityResultLauncher<Intent> avatarPickerLauncher;
    private ActivityResultLauncher<String> avatarPermissionLauncher;
    private boolean isAvatarUploading = false;
    private boolean isNotesLoading = false;
    private Uri tempAvatarUri = null; // 选图后的临时头像，在上传完成前使用

    private TokenStore tokenStore;
    private UserStore userStore;
    private LikedStore likedStore;
    private AliNoteRepository noteRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        tokenStore = TokenStore.getInstance(this);
        userStore = UserStore.getInstance(this);
        likedStore = LikedStore.getInstance(this);
        noteRepository = AliNoteRepository.getInstance(this);

        toolbar = findViewById(R.id.toolbar_profile);
        ivAvatar = findViewById(R.id.iv_avatar);
        tvUsername = findViewById(R.id.tv_username);
        tvAccountId = findViewById(R.id.tv_account_id);
        tvSignature = findViewById(R.id.tv_signature);
        btnProfileAction = findViewById(R.id.btn_profile_action);
        tabLayout = findViewById(R.id.tab_layout);
        contentContainer = findViewById(R.id.content_container);
        tvEmpty = findViewById(R.id.tv_empty);
        tvLikesListPlaceholder = findViewById(R.id.tv_likes_list_placeholder);
        rvMyNotes = findViewById(R.id.rv_my_notes);
        progressBar = findViewById(R.id.progress_loading);

        rvMyNotes.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));
        myCardsAdapter = new NoteCardAdapter(this, myRegionItems, null);
        likedCardsAdapter = new NoteCardAdapter(this, likedRegionItems, this::handleUnlikeFromLikes);
        rvMyNotes.setAdapter(myCardsAdapter);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    rvMyNotes.setAdapter(myCardsAdapter);
                    rvMyNotes.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));
                    rvMyNotes.setVisibility(View.VISIBLE);
                    tvEmpty.setVisibility(View.GONE);
                    tvLikesListPlaceholder.setVisibility(View.GONE);
                    loadMyNotes();
                } else {
                    rvMyNotes.setAdapter(likedCardsAdapter);
                    rvMyNotes.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));
                    rvMyNotes.setVisibility(View.VISIBLE);
                    tvEmpty.setVisibility(View.GONE);
                    tvLikesListPlaceholder.setVisibility(View.GONE);
                    loadLikedNotes();
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) { }

            @Override
            public void onTabReselected(TabLayout.Tab tab) { }
        });

        initAvatarPicker();

        ivAvatar.setOnClickListener(v -> handleAvatarClick());
        tvUsername.setOnClickListener(v -> handleNicknameClick());
    }

    @Override
    protected void onStart() {
        super.onStart();
        updateProfileUI();
        loadMyNotes();
        // 预加载点赞列表，避免切换时等待
        loadLikedNotes();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    private void updateProfileUI() {
        String token = tokenStore.getToken();
        if (TextUtils.isEmpty(token)) {
            tvUsername.setText("未登录用户");
            tvAccountId.setText("点击头像登录");
            tvSignature.setText("登录后可同步发布和点赞记录");
            btnProfileAction.setText("去登录");
            btnProfileAction.setOnClickListener(v -> startLogin());
            Glide.with(this).load(R.drawable.ic_profile).into(ivAvatar);
            return;
        }

        String tokenUid = userStore.extractUidFromToken(token);
        LoginResponse.UserDto user = userStore.getUser();
        if (!TextUtils.isEmpty(tokenUid) && (user == null || TextUtils.isEmpty(user.uid) || !tokenUid.equals(user.uid))) {
            LoginResponse.UserDto patched = user != null ? user : new LoginResponse.UserDto();
            patched.uid = tokenUid;
            if (TextUtils.isEmpty(patched.username)) {
                patched.username = "已登录用户";
            }
            patched = userStore.saveUser(patched);
            user = patched;
        }
        String uid = user != null ? user.uid : null;
        uid = userStore.ensureUid(uid);
        String username = user != null ? user.username : null;
        String avatarUrl = user != null ? user.avatarUrl : null;

        tvUsername.setText(TextUtils.isEmpty(username) ? "已登录用户" : username);
        tvAccountId.setText("UID: " + (TextUtils.isEmpty(uid) ? "未知" : uid));
        tvSignature.setText("欢迎回来");
        btnProfileAction.setText("退出登录");
        btnProfileAction.setOnClickListener(v -> {
            tokenStore.clear();
            userStore.clear();
            isNotesLoading = false;
            isAvatarUploading = false;
            updateLoadingState();
            Toast.makeText(this, "已退出登录", Toast.LENGTH_SHORT).show();
            updateProfileUI();
            myRegionItems.clear();
            likedRegionItems.clear();
            myCardsAdapter.notifyDataSetChanged();
            likedCardsAdapter.notifyDataSetChanged();
            tvEmpty.setVisibility(View.VISIBLE);
            rvMyNotes.setVisibility(View.GONE);
        });
        // 上传中时优先使用本地选图的临时头像，避免被占位图覆盖
        if (tempAvatarUri != null && isAvatarUploading) {
            Glide.with(this)
                    .load(tempAvatarUri)
                    .placeholder(R.drawable.ic_profile)
                    .error(R.drawable.ic_profile)
                    .circleCrop()
                    .into(ivAvatar);
        } else {
            Glide.with(this)
                    .load(buildAvatarGlideModel(avatarUrl))
                    .placeholder(R.drawable.ic_profile)
                    .error(R.drawable.ic_profile)
                    .circleCrop()
                    .into(ivAvatar);
        }
    }

    private void handleAvatarClick() {
        if (TextUtils.isEmpty(tokenStore.getToken())) {
            startLogin();
            return;
        }
        ensureAvatarPermissionAndPick();
    }

    private void handleNicknameClick() {
        if (TextUtils.isEmpty(tokenStore.getToken())) {
            startLogin();
            return;
        }
        showEditNicknameDialog();
    }

    private void startLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadMyNotes() {
        String token = tokenStore.getToken();
        String uid = userStore.getUid();
        if (TextUtils.isEmpty(token) || TextUtils.isEmpty(uid)) {
            rvMyNotes.setVisibility(View.GONE);
            tvEmpty.setText("登录后查看我的作品");
            tvEmpty.setVisibility(View.VISIBLE);
            return;
        }
        isNotesLoading = true;
        updateLoadingState();
        noteRepository.fetchAllNotes(new AliNoteRepository.MapNotesCallback() {
            @Override
            public void onSuccess(List<MapNote> notes) {
                runOnUiThread(() -> {
                    isNotesLoading = false;
                    updateLoadingState();
                    myRegionItems.clear();
                    for (MapNote n : notes) {
                        if (uid.equals(n.getAuthorId()) || uid.equals(n.getAuthorName())) {
                            RegionItem item = n.toRegionItem();
                            myRegionItems.add(item);
                        }
                    }
                    if (myRegionItems.isEmpty()) {
                        tvEmpty.setText("暂无作品");
                        tvEmpty.setVisibility(View.VISIBLE);
                        rvMyNotes.setVisibility(View.GONE);
                    } else {
                        tvEmpty.setVisibility(View.GONE);
                        rvMyNotes.setVisibility(View.VISIBLE);
                        myCardsAdapter.notifyDataSetChanged();
                    }
                });
            }

            @Override
            public void onError(@NonNull Throwable throwable) {
                runOnUiThread(() -> {
                    isNotesLoading = false;
                    updateLoadingState();
                    tvEmpty.setText("加载失败: " + throwable.getMessage());
                    tvEmpty.setVisibility(View.VISIBLE);
                    rvMyNotes.setVisibility(View.GONE);
                });
            }
        });
    }

    private void loadLikedNotes() {
        String token = tokenStore.getToken();
        String uid = userStore.getUid();
        if (TextUtils.isEmpty(token) || TextUtils.isEmpty(uid)) {
            if (tabLayout.getSelectedTabPosition() == 1) {
                rvMyNotes.setVisibility(View.GONE);
                tvEmpty.setText("登录后查看点赞作品");
                tvEmpty.setVisibility(View.VISIBLE);
            }
            return;
        }
        isNotesLoading = true;
        updateLoadingState();
        Set<String> likedIds = likedStore.getLikedIds(uid);
        if (likedIds.isEmpty()) {
            isNotesLoading = false;
            updateLoadingState();
            if (tabLayout.getSelectedTabPosition() == 1) {
                likedRegionItems.clear();
                likedCardsAdapter.notifyDataSetChanged();
                rvMyNotes.setVisibility(View.GONE);
                tvEmpty.setText("暂无点赞");
                tvEmpty.setVisibility(View.VISIBLE);
            }
            return;
        }
        noteRepository.fetchAllNotes(new AliNoteRepository.MapNotesCallback() {
            @Override
            public void onSuccess(List<MapNote> notes) {
                runOnUiThread(() -> {
                    isNotesLoading = false;
                    updateLoadingState();
                    likedRegionItems.clear();
                    for (MapNote n : notes) {
                        if (n != null && n.getId() != null && likedIds.contains(n.getId())) {
                            RegionItem item = n.toRegionItem();
                            item.setLikedByCurrentUser(true);
                            likedRegionItems.add(item);
                        }
                    }
                    likedCardsAdapter.notifyDataSetChanged();
                    if (tabLayout.getSelectedTabPosition() == 1) {
                        if (likedRegionItems.isEmpty()) {
                            rvMyNotes.setVisibility(View.GONE);
                            tvEmpty.setText("暂无点赞");
                            tvEmpty.setVisibility(View.VISIBLE);
                        } else {
                            tvEmpty.setVisibility(View.GONE);
                            rvMyNotes.setVisibility(View.VISIBLE);
                        }
                    }
                });
            }

            @Override
            public void onError(@NonNull Throwable throwable) {
                runOnUiThread(() -> {
                    isNotesLoading = false;
                    updateLoadingState();
                    if (tabLayout.getSelectedTabPosition() == 1) {
                        tvEmpty.setText("加载失败: " + throwable.getMessage());
                        tvEmpty.setVisibility(View.VISIBLE);
                        rvMyNotes.setVisibility(View.GONE);
                    }
                });
            }
        });
    }

    private void handleUnlikeFromLikes(RegionItem regionItem) {
        if (regionItem == null) return;
        AliNoteRepository.LikeCallback callback = new AliNoteRepository.LikeCallback() {
            @Override
            public void onResult(boolean liked, int likeCount) {
                likedStore.saveLikeCount(regionItem.getNoteId(), likeCount);
                if (!liked) {
                    int idx = removeRegionItem(regionItem.getNoteId());
                    if (idx == -1) return;
                    likedCardsAdapter.notifyItemRemoved(idx);
                    if (likedRegionItems.isEmpty()) {
                        rvMyNotes.setVisibility(View.GONE);
                        tvEmpty.setText("暂无点赞");
                        tvEmpty.setVisibility(View.VISIBLE);
                    }
                    // 让首页等列表绑定时能拿到最新计数
                    likedStore.toggle(userStore.getUid(), regionItem.getNoteId(), false);
                } else {
                    likedStore.toggle(userStore.getUid(), regionItem.getNoteId(), true);
                    loadLikedNotes();
                }
            }

            @Override
            public void onRequireLogin() {
                startLogin();
            }

            @Override
            public void onError(@NonNull Throwable throwable) {
                Toast.makeText(ProfileActivity.this, "取消点赞失败: " + throwable.getMessage(), Toast.LENGTH_SHORT).show();
            }
        };
        noteRepository.toggleLike(regionItem, callback);
    }

    private int removeRegionItem(String noteId) {
        for (int i = 0; i < likedRegionItems.size(); i++) {
            if (noteId.equals(likedRegionItems.get(i).getNoteId())) {
                likedRegionItems.remove(i);
                return i;
            }
        }
        return -1;
    }

    private void openNoteDetail(RegionItem item) {
        if (item == null) return;
        Intent intent = new Intent(this, NoteDetailActivity.class);
        intent.putExtra(NoteDetailActivity.EXTRA_NOTE_DATA, item);
        startActivity(intent);
    }

    private void initAvatarPicker() {
        avatarPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            tempAvatarUri = uri;
                            // 先本地预览，避免上传成功前显示为空
                            ivAvatar.setImageURI(uri);
                            ivAvatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
                            uploadAvatar(uri);
                        }
                    }
                });

        avatarPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (granted) {
                        openGalleryForAvatar();
                    } else {
                        Toast.makeText(this, "需要相册权限以更换头像", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void ensureAvatarPermissionAndPick() {
        String permission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ? Manifest.permission.READ_MEDIA_IMAGES
                : Manifest.permission.READ_EXTERNAL_STORAGE;
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            openGalleryForAvatar();
        } else {
            avatarPermissionLauncher.launch(permission);
        }
    }

    private void openGalleryForAvatar() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        avatarPickerLauncher.launch(intent);
    }

    private void uploadAvatar(Uri uri) {
        if (uri == null || isAvatarUploading) return;
        isAvatarUploading = true;
        updateLoadingState();
        noteRepository.uploadImage(uri, new AliNoteRepository.UploadCallback() {
            @Override
            public void onSuccess(String fileUrl) {
                runOnUiThread(() -> {
                    isAvatarUploading = false;
                    updateLoadingState();
                    userStore.updateAvatar(fileUrl);
                    tempAvatarUri = null;
                    updateProfileUI();
                    Toast.makeText(ProfileActivity.this, "头像已更新", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onError(@NonNull Throwable throwable) {
                runOnUiThread(() -> {
                    isAvatarUploading = false;
                    updateLoadingState();
                    Toast.makeText(ProfileActivity.this, "头像更新失败: " + throwable.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void showEditNicknameDialog() {
        String currentName = userStore.getUsername();
        final EditText input = new EditText(this);
        input.setHint("请输入昵称");
        if (!TextUtils.isEmpty(currentName)) {
            input.setText(currentName);
            input.setSelection(currentName.length());
        }
        new AlertDialog.Builder(this)
                .setTitle("修改昵称")
                .setView(input)
                .setPositiveButton("保存", (dialog, which) -> {
                    String newName = input.getText() != null ? input.getText().toString().trim() : "";
                    if (TextUtils.isEmpty(newName)) {
                        Toast.makeText(this, "昵称不能为空", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    userStore.updateUsername(newName);
                    updateProfileUI();
                    Toast.makeText(this, "昵称已更新", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void updateLoadingState() {
        progressBar.setVisibility((isAvatarUploading || isNotesLoading) ? View.VISIBLE : View.GONE);
    }

    private Object buildAvatarGlideModel(String avatarUrl) {
        if (TextUtils.isEmpty(avatarUrl)) {
            return R.drawable.ic_profile;
        }
        String url = normalizeOssUrl(avatarUrl);
        return new GlideUrl(url, new LazyHeaders.Builder()
                .addHeader("Referer", "http://notemap-prod-oss.oss-cn-beijing.aliyuncs.com")
                .build());
    }

    private String normalizeOssUrl(String url) {
        if (TextUtils.isEmpty(url)) return url;
        if (url.startsWith("https://notemap-prod-oss.oss-cn-beijing.aliyuncs.com")) {
            return url.replaceFirst("^https://", "http://");
        }
        return url;
    }
}
