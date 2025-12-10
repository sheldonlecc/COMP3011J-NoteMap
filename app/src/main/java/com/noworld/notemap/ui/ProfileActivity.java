package com.noworld.notemap.ui;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.tabs.TabLayout;
import com.noworld.notemap.R;
import com.noworld.notemap.data.AliNoteRepository;
import com.noworld.notemap.data.LikedStore;
import com.noworld.notemap.data.MapNote;
import com.noworld.notemap.data.TokenStore;
import com.noworld.notemap.data.UserStore;
import com.noworld.notemap.data.dto.LoginResponse;
import com.amap.apis.cluster.demo.RegionItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

// 引入高斯模糊库
import jp.wasabeef.glide.transformations.BlurTransformation;
import com.bumptech.glide.load.MultiTransformation;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import android.graphics.Bitmap;

/**
 * 个人主页 Activity
 */
public class ProfileActivity extends AppCompatActivity {

    private Toolbar toolbar;

    // 【新增】背景图相关控件
    private FrameLayout headerContainer;
    private ImageView ivProfileBackground;

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

    // 【新增】背景图选择器
    private ActivityResultLauncher<Intent> backgroundPickerLauncher;

    private ActivityResultLauncher<Intent> avatarPickerLauncher;
    private ActivityResultLauncher<String> permissionLauncher;

    // 【新增】标记当前是选头像还是选背景 (true=头像, false=背景)
    private boolean isPickingAvatar = true;

    private boolean isAvatarUploading = false;
    private boolean isNotesLoading = false;
    private Uri tempAvatarUri = null;

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

        // 【新增 1】绑定新布局中的控件
        headerContainer = findViewById(R.id.header_container);
        ivProfileBackground = findViewById(R.id.iv_profile_background);

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
            // 隐藏标题，避免遮挡背景
            getSupportActionBar().setTitle("");
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

        // 初始化所有选择器
        initPickers();

        ivAvatar.setOnClickListener(v -> handleAvatarClick());
        tvUsername.setOnClickListener(v -> handleNicknameClick());

        // 【新增 2】点击头部背景区域触发更换背景
        headerContainer.setOnClickListener(v -> handleBackgroundClick());

        // 【新增 3】加载上次保存的背景图
        loadProfileBackground();
    }

    // ===========================================
    // 【新增方法区】处理背景图逻辑
    // ===========================================

    private void initPickers() {
        // 1. 头像选择回调
        avatarPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            tempAvatarUri = uri;
                            ivAvatar.setImageURI(uri);
                            ivAvatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
                            uploadAvatar(uri);
                        }
                    }
                });

        // 2. 【新增】背景图选择回调
        backgroundPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            // A. 加载并模糊显示
                            loadBlurBackground(uri);
                            // B. 本地保存
                            userStore.setProfileBg(uri.toString());
                            Toast.makeText(this, "Background updated", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        // 3. 权限请求回调
        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (granted) {
                        openGallery();
                    } else {
                        Toast.makeText(this, "Photo permission required to choose an image", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // 加载本地存储的背景
    private void loadProfileBackground() {
        String bgUriString = userStore.getProfileBg();
        if (!TextUtils.isEmpty(bgUriString)) {
            loadBlurBackground(Uri.parse(bgUriString));
        } else {
            // 默认背景色
            ivProfileBackground.setImageResource(R.color.colorPrimary);
        }
    }

    // 使用 Glide 加载并应用高斯模糊
    private void loadBlurBackground(Object model) {
        if (model == null) return;

        // 定义组合变换：先居中裁切(CenterCrop)，再高斯模糊(Blur)
        // 这样可以保证无论图片多长，都会先自动裁切成合适比例，再进行模糊
        MultiTransformation<Bitmap> multi = new MultiTransformation<>(
                new CenterCrop(),
                new jp.wasabeef.glide.transformations.BlurTransformation(25, 3)
        );

        Glide.with(this)
                .load(model)
                .apply(RequestOptions.bitmapTransform(multi))
                .into(ivProfileBackground);
    }

    // 点击背景触发
    private void handleBackgroundClick() {
        new AlertDialog.Builder(this)
                .setTitle("Change background")
                .setMessage("Replace the profile background image?")
                .setPositiveButton("Change", (dialog, which) -> {
                    isPickingAvatar = false; // 标记：我在选背景
                    ensurePermissionAndPick();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ===========================================

    private void handleAvatarClick() {
        if (TextUtils.isEmpty(tokenStore.getToken())) {
            startLogin();
            return;
        }
        isPickingAvatar = true; // 标记：我在选头像
        ensurePermissionAndPick();
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

    private void ensurePermissionAndPick() {
        String permission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ? Manifest.permission.READ_MEDIA_IMAGES
                : Manifest.permission.READ_EXTERNAL_STORAGE;
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            openGallery();
        } else {
            permissionLauncher.launch(permission);
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        // 根据标记判断是启动哪个选择器
        if (isPickingAvatar) {
            avatarPickerLauncher.launch(intent);
        } else {
            backgroundPickerLauncher.launch(intent);
        }
    }

    private void uploadAvatar(Uri uri) {
        if (uri == null || isAvatarUploading) return;
        isAvatarUploading = true;
        updateLoadingState();

        // 1. 上传 OSS
        noteRepository.uploadImage(uri, new AliNoteRepository.UploadCallback() {
            @Override
            public void onSuccess(String fileUrl) {
                // 2. OSS 成功后，同步给后端数据库
                noteRepository.updateUserInfo(null, fileUrl, new AliNoteRepository.SimpleCallback() {
                    @Override
                    public void onSuccess() {
                        runOnUiThread(() -> {
                            isAvatarUploading = false;
                            updateLoadingState();

                            // 3. 后端成功后，更新本地
                            userStore.updateAvatar(fileUrl);
                            tempAvatarUri = null;
                            updateProfileUI();
                            Toast.makeText(ProfileActivity.this, "Avatar synced to cloud", Toast.LENGTH_SHORT).show();
                        });
                    }

                    @Override
                    public void onError(Throwable t) {
                        runOnUiThread(() -> {
                            isAvatarUploading = false;
                            updateLoadingState();
                            Toast.makeText(ProfileActivity.this, "Cloud sync failed: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        });
                    }
                });
            }

            @Override
            public void onError(@NonNull Throwable throwable) {
                runOnUiThread(() -> {
                    isAvatarUploading = false;
                    updateLoadingState();
                    Toast.makeText(ProfileActivity.this, "Image upload failed: " + throwable.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void showEditNicknameDialog() {
        String currentName = userStore.getUsername();
        final EditText input = new EditText(this);
        input.setHint("Enter nickname");
        if (!TextUtils.isEmpty(currentName)) {
            input.setText(currentName);
            input.setSelection(currentName.length());
        }
        new AlertDialog.Builder(this)
                .setTitle("Edit nickname")
                .setView(input)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newName = input.getText() != null ? input.getText().toString().trim() : "";
                    if (TextUtils.isEmpty(newName)) {
                        Toast.makeText(this, "Nickname cannot be empty", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // 调用后端保存昵称
                    noteRepository.updateUserInfo(newName, null, new AliNoteRepository.SimpleCallback() {
                        @Override
                        public void onSuccess() {
                            runOnUiThread(() -> {
                                userStore.updateUsername(newName);
                                updateProfileUI();
                                Toast.makeText(ProfileActivity.this, "Nickname updated", Toast.LENGTH_SHORT).show();
                            });
                        }

                        @Override
                        public void onError(Throwable t) {
                            runOnUiThread(() -> {
                                Toast.makeText(ProfileActivity.this, "Update failed: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateLoadingState() {
        progressBar.setVisibility((isAvatarUploading || isNotesLoading) ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onStart() {
        super.onStart();
        updateProfileUI();
        loadMyNotes();
        loadLikedNotes();
    }

    private void updateProfileUI() {
        String token = tokenStore.getToken();
        if (TextUtils.isEmpty(token)) {
            tvUsername.setText("Guest");
            tvAccountId.setText("Tap avatar to log in");
            tvSignature.setText("Log in to sync posts and likes");
            btnProfileAction.setText("Log in");
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
                patched.username = "Logged-in user";
            }
            patched = userStore.saveUser(patched);
            user = patched;
        }
        String uid = user != null ? user.uid : null;
        uid = userStore.ensureUid(uid);
        String username = user != null ? user.username : null;
        String avatarUrl = user != null ? user.avatarUrl : null;

        tvUsername.setText(TextUtils.isEmpty(username) ? "Logged-in user" : username);
        tvAccountId.setText("UID: " + (TextUtils.isEmpty(uid) ? "Unknown" : uid));
        tvSignature.setText("Welcome back");

        // 【视觉优化】确保文字在背景图上可见
        tvUsername.setTextColor(ContextCompat.getColor(this, R.color.white));
        tvAccountId.setTextColor(0xDDFFFFFF);
        tvSignature.setTextColor(0xDDFFFFFF);

        btnProfileAction.setText("Log out");
        btnProfileAction.setOnClickListener(v -> {
            tokenStore.clear();
            userStore.clear();
            isNotesLoading = false;
            isAvatarUploading = false;
            updateLoadingState();
            Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show();
            updateProfileUI();
            myRegionItems.clear();
            likedRegionItems.clear();
            myCardsAdapter.notifyDataSetChanged();
            likedCardsAdapter.notifyDataSetChanged();
            tvEmpty.setVisibility(View.VISIBLE);
            rvMyNotes.setVisibility(View.GONE);
        });

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

    private void loadMyNotes() {
        String token = tokenStore.getToken();
        String uid = userStore.getUid();
        if (TextUtils.isEmpty(token) || TextUtils.isEmpty(uid)) {
            rvMyNotes.setVisibility(View.GONE);
            tvEmpty.setText("Log in to view my notes");
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
                        tvEmpty.setText("No works yet");
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
                    tvEmpty.setText("Load failed: " + throwable.getMessage());
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
                tvEmpty.setText("Log in to view liked notes");
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
                tvEmpty.setText("No likes yet");
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
                            tvEmpty.setText("No likes yet");
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
                        tvEmpty.setText("Load failed: " + throwable.getMessage());
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
                        tvEmpty.setText("No likes yet");
                        tvEmpty.setVisibility(View.VISIBLE);
                    }
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
                Toast.makeText(ProfileActivity.this, "Failed to remove like: " + throwable.getMessage(), Toast.LENGTH_SHORT).show();
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
