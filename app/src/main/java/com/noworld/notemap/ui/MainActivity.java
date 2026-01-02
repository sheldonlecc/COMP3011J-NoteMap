package com.noworld.notemap.ui;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Rect;
import android.graphics.Path;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.cursoradapter.widget.CursorAdapter;
import androidx.cursoradapter.widget.SimpleCursorAdapter;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.LocationSource;
import com.amap.api.maps.MapView;
import com.amap.api.maps.UiSettings;
import com.amap.api.maps.model.BitmapDescriptor;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MyLocationStyle;
import com.amap.apis.cluster.Cluster;
import com.amap.apis.cluster.ClusterClickListener;
import com.amap.apis.cluster.ClusterItem;
import com.amap.apis.cluster.ClusterOverlay;
import com.amap.apis.cluster.ClusterRender;
import com.amap.apis.cluster.demo.RegionItem;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.FutureTarget;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.geocoder.GeocodeResult;
import com.amap.api.services.geocoder.GeocodeSearch;
import com.amap.api.services.geocoder.RegeocodeResult;
import com.amap.api.services.geocoder.GeocodeQuery;
import com.amap.api.services.help.Inputtips;
import com.amap.api.services.help.InputtipsQuery;
import com.amap.api.services.help.Tip;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.noworld.notemap.R;
import com.noworld.notemap.data.AliNoteRepository;
import com.noworld.notemap.data.ApiClient;
import com.noworld.notemap.data.ApiService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.noworld.notemap.data.UserStore; // 导入您的 UserStore
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

// [重要] 确保类声明实现了所有接口
public class MainActivity extends AppCompatActivity implements AMapLocationListener, LocationSource, View.OnClickListener, ClusterRender, ClusterClickListener, GeocodeSearch.OnGeocodeSearchListener {

    private static final String TAG = "MainActivity";
    private UserStore userStore;

    // 请求权限意图
    private ActivityResultLauncher<String> requestPermission;

    //声明AMapLocationClient类对象
    public AMapLocationClient mLocationClient = null;
    //声明AMapLocationClientOption对象
    public AMapLocationClientOption mLocationOption = null;
    // 声明地图控制器
    private AMap aMap = null;
    // 声明地图定位监听
    private LocationSource.OnLocationChangedListener mListener = null;
    // 声明当前坐标
    private LatLng latLng = null;

    // 声明是否设置缩放级别
    private boolean isSetZoomLevel = false;
    private UiSettings mUiSettings;

    private MapView mp_view;
    // [修改] 保留右上角的 FAB
    private FloatingActionButton fab_zoom_large;
    private FloatingActionButton fab_zoom_small;
    private FloatingActionButton fab_location;

    // [修改] 新的底部 FAB 按钮变量 (类型从 ImageButton 改为 FloatingActionButton)
    private FloatingActionButton fab_my_location;
    private FloatingActionButton fab_add_note;
    private FloatingActionButton fab_user_profile;
    private SearchView searchView;
    private View bottomSheetSearch;
    private RecyclerView rvSearchResult;
    private TextView tvSearchResultTitle;
    private BottomSheetBehavior<View> searchSheetBehavior;
    private List<RegionItem> searchSheetNotes = new ArrayList<>();
    private NoteCardAdapter searchSheetAdapter;

    private androidx.appcompat.widget.Toolbar toolbar_main; // 添加这个变量

    // 点聚合相关
    private ClusterOverlay mClusterOverlay;
    private int clusterRadius = 100; // 聚合半径 (dp)
    private final Map<String, BitmapDescriptor> clusterCircleCache = new HashMap<>();
    private final LruCache<String, BitmapDescriptor> markerIconCache = new LruCache<>(120); // 自定义 Marker 缓存
    private final List<RegionItem> latestNotes = new ArrayList<>();
    private final List<RegionItem> displayedNotes = new ArrayList<>();
    private static final String[] ALL_NOTE_TYPES = new String[]{
            "Recommendation", "Guide", "Review", "Share", "Collection", "Tutorial", "Unboxing", "Vlog", "Store visit"
    };
    private String currentKeyword = "";
    private final Set<String> currentTypeFilters = new HashSet<>();
    private boolean isMapLoaded = false;

    // 高德模糊搜索
    private static final String[] SEARCH_SUGGEST_COLUMNS = new String[]{"_id", "suggest_text_1", "suggest_text_2"};
    private SimpleCursorAdapter suggestionAdapter;
    private final List<Tip> currentTips = new ArrayList<>();
    private Inputtips inputtips;

    // 数据层
    private AliNoteRepository noteRepository;
    private GeocodeSearch geocodeSearch;
    private Marker searchMarker;

    // Marker 渲染
    private LayoutInflater markerLayoutInflater;
    private Bitmap fallbackMarkerBitmap;

    private TextView tvDetailTime;
    private RecyclerView rvComments;
    private TextView tvInputComment;
    private TextView tvLikeCount;
    private TextView tvCommentCount;
    private View notificationContainer;
    private TextView tvNotificationBadge;
    private NotificationBadgeHelper badgeHelper;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        noteRepository = AliNoteRepository.getInstance(this);
        markerLayoutInflater = LayoutInflater.from(this);
        fallbackMarkerBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_car);

        initView();

        setSupportActionBar(toolbar_main);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("");
        }

        requestPermission = registerForActivityResult(new ActivityResultContracts.RequestPermission(), result -> {
            // 权限申请结果
            Log.d(TAG, "Permission request result: " + result);
            showMsg(result ? "Location permission granted" : "Permission request failed");

        });

        // 初始化定位
        initLocation();
        mp_view.onCreate(savedInstanceState);
        // 初始化地图
        initMap();
        initGeocoder();

        subscribeNotes();

        // [删除] 移除了 initMarker() 和 initImageLauncher() (车辆逻辑)

        Log.d(TAG, "moveCamera: " + latLng);
        // 设置地图默认缩放级别 (可以调整为更远的 10)
        aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(39.904, 116.407), 10));
        Log.d(TAG, "moveEnd!");

        userStore = UserStore.getInstance(this);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //第二个参数表示此menu的id值，在onOptionsItemSelected方法中通过id值判断是哪个menu被点击了
        menu.add(Menu.NONE, 1, 1, "Normal view");
        menu.add(Menu.NONE, 2, 2, "Night view");
        menu.add(Menu.NONE, 3, 3, "Satellite view");
        menu.add(Menu.NONE, 4, 4, "Navigation view");
        return true;
    }


    /**
     * 点击实现的操作
     */
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case 1:
                aMap.setMapType(AMap.MAP_TYPE_NORMAL);
                showMsg("Switched to normal view");
                break;
            case 2:
                aMap.setMapType(AMap.MAP_TYPE_NIGHT);
                showMsg("Switched to night view");
                break;
            case 3:
                aMap.setMapType(AMap.MAP_TYPE_SATELLITE);
                showMsg("Switched to satellite view");
                break;
            case 4:
                aMap.setMapType(AMap.MAP_TYPE_NAVI);
                showMsg("Switched to navigation view");
                break;
        }
        return true;
    }

    /**
     * 初始化定位
     */
    private void initLocation() {
        try {
            //初始化定位
            mLocationClient = new AMapLocationClient(getApplicationContext());
            //初始化定位参数
            mLocationOption = new AMapLocationClientOption();
            //设置定位回调监听
            mLocationClient.setLocationListener(this);
            //设置定位模式为AMapLocationMode.Hight_Accuracy，高精度模式。
            mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
            //获取最近3s内精度最高的一次定位结果
            mLocationOption.setOnceLocationLatest(true);
            //设置是否返回地址信息（默认返回地址信息）
            mLocationOption.setNeedAddress(true);
            //设置定位超时时间，单位是毫秒
            mLocationOption.setHttpTimeOut(6000);
            //给定位客户端对象设置定位参数
            mLocationClient.setLocationOption(mLocationOption);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 初始化地图
     */
    private void initMap() {
        Log.d(TAG, "initMap");
        if (aMap == null) {
            aMap = mp_view.getMap();
            // [修改 1] 明确启用旋转手势
            mUiSettings = aMap.getUiSettings();
            mUiSettings.setZoomControlsEnabled(false);
            mUiSettings.setRotateGesturesEnabled(true); // [新增] 允许用户手动旋转地图
            // 创建定位蓝点的样式
            MyLocationStyle myLocationStyle = new MyLocationStyle();
            // 自定义精度范围的圆形边框颜色  都为0则透明
            myLocationStyle.strokeColor(Color.argb(0, 0, 0, 0));
            // 自定义精度范围的圆形边框宽度  0 无宽度
            myLocationStyle.strokeWidth(0);
            // 设置连续定位模式下的定位间隔，只在连续定位模式下生效，单次定位模式下不会生效。单位为毫秒。
            myLocationStyle.interval(1000);
            // 设置圆形的填充颜色  都为0则透明
            myLocationStyle.radiusFillColor(Color.argb(0, 0, 0, 0));

            // [修改]
            myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE_NO_CENTER);
            // 恢复默认锚点，避免偏移过大
            myLocationStyle.anchor(0.5f, 0.5f);

            // 设置定位蓝点的样式
            aMap.setMyLocationStyle(myLocationStyle);
            // 设置默认定位按钮是否显示
            // aMap.getUiSettings().setMyLocationButtonEnabled(true);
            // 设置定位监听
            aMap.setLocationSource(this);
            // 设置为true表示启动显示定位蓝点，false表示隐藏定位蓝点并不进行定位，默认是false。
            aMap.setMyLocationEnabled(true);

            // 开启室内地图
            aMap.showIndoorMap(true);

            // [新增] 注册地图加载完成监听，用于初始化点聚合
            aMap.setOnMapLoadedListener(this::initClusterData);

            // 地图控件设置
            UiSettings uiSettings = aMap.getUiSettings();
            // 隐藏缩放按钮
            uiSettings.setZoomControlsEnabled(false);
            // 显示比例尺，默认不显示
            uiSettings.setScaleControlsEnabled(true);
            // 显示指南针
            uiSettings.setCompassEnabled(true);

        }
    }


    /**
     * 开始定位
     */
    private void startLocation() {
        if (mLocationClient != null) mLocationClient.startLocation();
    }

    /**
     * 停止定位
     */
    private void stopLocation() {
        if (mLocationClient != null) mLocationClient.stopLocation();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mp_view.onResume();
        // 检查是否已经获取到定位权限
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            startLocation();
        } else {
            requestPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        // 重新拉取笔记，确保发布后返回主页能看到最新数据
        subscribeNotes();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mp_view.onPause();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mp_view.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // [新增] 销毁聚合图层
        clearClusterOverlay();
        markerIconCache.evictAll();
        clusterCircleCache.clear();
        mp_view.onDestroy();
        stopLocation();
        if (mLocationClient != null) {
            mLocationClient.onDestroy();
        }
        if (searchMarker != null) {
            searchMarker.remove();
            searchMarker = null;
        }
    }


    private void showMsg(CharSequence llw) {
        Toast.makeText(this, llw, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onLocationChanged(AMapLocation aMapLocation) {
        if (aMapLocation == null) {
            showMsg("Location failed: aMapLocation is null");
            Log.e(TAG, "Location failed: aMapLocation is null");
            return;
        }
        // 获取定位结果
        if (aMapLocation.getErrorCode() == 0) {
            // 定位成功
            Log.i(TAG, "Location succeeded");
            latLng = new LatLng(aMapLocation.getLatitude(), aMapLocation.getLongitude()); // 获取当前latlng坐标

            // 显示地图定位结果
            if (mListener != null) {
                mListener.onLocationChanged(aMapLocation);
            }
            if (!isSetZoomLevel && latLng != null) {
                // aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 18));
                // Log.d(TAG, "moveEnd!");
                // isSetZoomLevel = true;
                // [修改] 不再自动缩放到18级，让点聚合的默认缩放生效
            }
        } else {
            // 定位失败
            showMsg("Location failed: " + aMapLocation.getErrorInfo());
            Log.e(TAG, "location Error, ErrCode:"
                    + aMapLocation.getErrorCode() + ", errInfo:"
                    + aMapLocation.getErrorInfo());
        }
    }

    private void initView() {
        mp_view = findViewById(R.id.mp_view);
        searchView = findViewById(R.id.search_view);
        // [修改] 只绑定保留的 FAB
        fab_zoom_large = findViewById(R.id.fab_zoom_large);
        fab_zoom_small = findViewById(R.id.fab_zoom_small);
        fab_location = findViewById(R.id.fab_location);

        // [新增] 绑定 Toolbar
        toolbar_main = findViewById(R.id.toolbar_main);

        // [修改] 只为保留的 FAB 设置监听
        fab_zoom_large.setOnClickListener(this);
        fab_zoom_small.setOnClickListener(this);
        fab_location.setOnClickListener(this);

        // [修改] 绑定新的底部导航栏 FAB 按钮 (ID 已修正)
        fab_my_location = findViewById(R.id.fab_my_location);
        fab_add_note = findViewById(R.id.fab_add_note);
        fab_user_profile = findViewById(R.id.fab_user_profile);
        bottomSheetSearch = findViewById(R.id.bottom_sheet_search);
        rvSearchResult = findViewById(R.id.rv_search_result);
        tvSearchResultTitle = findViewById(R.id.tv_search_result_title);
        notificationContainer = findViewById(R.id.notification_container);
        tvNotificationBadge = findViewById(R.id.tv_notification_badge);
        badgeHelper = new NotificationBadgeHelper(tvNotificationBadge);

        // 默认隐藏徽标
        badgeHelper.setCount(0);

        // [修改] 为新 FAB 按钮设置点击监听
        fab_my_location.setOnClickListener(this);
        fab_add_note.setOnClickListener(this);
        fab_user_profile.setOnClickListener(this);
        if (notificationContainer != null) {
            notificationContainer.setOnClickListener(v -> openNotificationPage());
            View fabNotification = findViewById(R.id.fab_notification);
            if (fabNotification != null) {
                fabNotification.setOnClickListener(v -> openNotificationPage());
            }
        }

        tvDetailTime = findViewById(R.id.tv_detail_time);
        rvComments = findViewById(R.id.rv_comments);
        tvInputComment = findViewById(R.id.tv_input_comment);
        tvLikeCount = findViewById(R.id.tv_detail_like_count);
        tvCommentCount = findViewById(R.id.tv_detail_comment_count);

        setupSearchSheet();

        if (searchView != null) {
            searchView.setIconifiedByDefault(false);
            searchView.setSubmitButtonEnabled(true); // 显示右侧放大镜按钮
            initFuzzySearch();
            tweakSearchIconPosition();
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    applySearchQuery(query);
                    performFuzzyLocationSearch(query);
                    if (!displayedNotes.isEmpty()) {
                        showSearchSheet();
                    }
                    searchView.clearFocus(); // 收起键盘，反馈搜索已触发
                    return true;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    applySearchQuery(newText);
                    requestInputTips(newText);
                    if (TextUtils.isEmpty(newText)) {
                        hideSearchSheet();
                    } else if (!displayedNotes.isEmpty()) {
                        showSearchSheet();
                    }
                    return true;
                }
            });
        }
    }

    // =========================================================================
    // [点聚合核心逻辑 - 唯一的方法定义]
    // =========================================================================

    /**
     * [唯一] 在地图加载完成后，初始化并开始计算点聚合数据
     */
    public void initClusterData() {
        Log.d(TAG, "Map loaded, preparing to render remote notes...");
        isMapLoaded = true;
        attachClusterOverlay();
    }

    private void subscribeNotes() {
        if (noteRepository == null) {
            return;
        }
        noteRepository.fetchNotes(null, null, null, null, new AliNoteRepository.NotesCallback() {
            @Override
            public void onSuccess(List<RegionItem> items) {
                runOnUiThread(() -> updateNoteData(items));
            }

            @Override
            public void onError(@NonNull Throwable throwable) {
                Log.e(TAG, "Failed to sync notes", throwable);
                runOnUiThread(() -> showMsg("Failed to sync notes"));
            }
        });

        fetchUnreadNotifications();
    }

    // 替换 MainActivity.java 中的 updateNoteData 方法
    /**
     * 更新笔记数据，并注入实时时间和当前用户信息
     */
    /**
     * 更新笔记数据
     * 现在直接使用服务器返回的数据，不再进行本地替换
     */
    /**
     * 更新笔记数据
     */
    private void updateNoteData(List<RegionItem> notes) {
        latestNotes.clear();

        // 【重要！】删除或注释掉时间格式化工具和时间变量的准备，因为不再需要本地生成时间。
        // SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        // long currentTimeMillis = System.currentTimeMillis();

        if (notes != null) {
            for (int i = 0; i < notes.size(); i++) {
                RegionItem item = notes.get(i);

                // --- 【核心修复】时间问题：删除以下三行强制覆盖时间的代码 ---
            /*
            // 之前是因为 RegionItem 有默认值 "2023-11-27"，导致逻辑跳过了更新。
            // 既然后端没返回时间，我们这里直接强制覆盖为当前时间！
            long fakeTime = currentTimeMillis - (i * 1000L * 60L * 5L); // 每个笔记错开5分钟
            item.setCreateTime(sdf.format(new Date(fakeTime)));
            */
                // --- 逻辑删除完毕。现在 item.getCreateTime() 将使用 MapNote 传入的后端时间 ---


                // --- 头像逻辑保持不变 (相信后端数据) ---
                // 只要后端返回了 authorAvatarUrl，这里就能自动显示
            }

            latestNotes.addAll(notes);
        }
        applyFilters();
    }

    private void applySearchQuery(String query) {
        currentKeyword = query != null ? query.trim() : "";
        applyFilters();
    }

    private void openNotificationPage() {
        Intent intent = new Intent(this, NotificationActivity.class);
        startActivity(intent);
        clearNotificationBadge();
    }

    private void clearNotificationBadge() {
        if (badgeHelper != null) {
            badgeHelper.setCount(0);
        }
        ApiService api = ApiClient.getService(this);
        api.readAllNotifications().enqueue(new retrofit2.Callback<Void>() {
            @Override
            public void onResponse(retrofit2.Call<Void> call, retrofit2.Response<Void> response) {
                // ignore
            }

            @Override
            public void onFailure(retrofit2.Call<Void> call, Throwable t) {
                // ignore
            }
        });
    }

    private void fetchUnreadNotifications() {
        ApiService api = com.noworld.notemap.data.ApiClient.getService(this);
        api.getNotificationUnreadCount().enqueue(new retrofit2.Callback<java.util.Map<String, Integer>>() {
            @Override
            public void onResponse(retrofit2.Call<java.util.Map<String, Integer>> call, retrofit2.Response<java.util.Map<String, Integer>> response) {
                if (!response.isSuccessful() || response.body() == null || badgeHelper == null) return;
                Integer cnt = response.body().get("count");
                badgeHelper.setCount(cnt != null ? cnt : 0);
            }

            @Override
            public void onFailure(retrofit2.Call<java.util.Map<String, Integer>> call, Throwable t) {
                // ignore
            }
        });
    }

    private void applyFilters() {
        displayedNotes.clear();
        if (!latestNotes.isEmpty()) {
            for (RegionItem item : latestNotes) {
                if (item == null) {
                    continue;
                }
                if (!currentTypeFilters.isEmpty()) {
                    if (TextUtils.isEmpty(item.getNoteType()) || !currentTypeFilters.contains(item.getNoteType())) {
                        continue;
                    }
                }
                if (!TextUtils.isEmpty(currentKeyword)) {
                    String keywordLower = currentKeyword.toLowerCase();
                    String title = item.getTitle() != null ? item.getTitle().toLowerCase() : "";
                    String desc = item.getDescription() != null ? item.getDescription().toLowerCase() : "";
                    String location = item.getLocationName() != null ? item.getLocationName().toLowerCase() : "";
                    if (!title.contains(keywordLower) && !desc.contains(keywordLower) && !location.contains(keywordLower)) {
                        continue;
                    }
                }
                displayedNotes.add(item);
            }
        }
        if (displayedNotes.isEmpty()) {
            clearClusterOverlay();
            hideSearchSheet();
            return;
        }
        attachClusterOverlay();
        if (!TextUtils.isEmpty(currentKeyword)) {
            showSearchSheet();
        }
    }

    private void launchSearchResultIfNeeded() {
        if (displayedNotes.isEmpty()) {
            return;
        }
        showSearchSheet();
    }

    private void performFuzzyLocationSearch(String keyword) {
        if (TextUtils.isEmpty(keyword)) {
            showMsg("No related place/note found");
            return;
        }
        // 优先使用高德原生模糊搜索结果
        if (!currentTips.isEmpty()) {
            handleTipSelection(0);
            return;
        }
        // 回落到地理编码
        if (geocodeSearch == null) {
            showMsg("No related place/note found");
            return;
        }
        GeocodeQuery query = new GeocodeQuery(keyword, "");
        geocodeSearch.getFromLocationNameAsyn(query);
    }

    private void showTypeFilterDialog() {
        List<String> types = new ArrayList<>();
        types.add("All");
        for (String t : ALL_NOTE_TYPES) {
            types.add(t);
        }
        CharSequence[] items = types.toArray(new CharSequence[0]);
        boolean[] checked = new boolean[items.length];
        for (int i = 1; i < items.length; i++) {
            if (currentTypeFilters.contains(items[i].toString())) {
                checked[i] = true;
            }
        }
        new AlertDialog.Builder(this)
                .setTitle("Filter by type")
                .setMultiChoiceItems(items, checked, (dialog, which, isChecked) -> {
                    if (which == 0) { // All
                        for (int i = 1; i < checked.length; i++) {
                            checked[i] = false;
                            ((AlertDialog) dialog).getListView().setItemChecked(i, false);
                        }
                        currentTypeFilters.clear();
                    } else {
                        String t = items[which].toString();
                        if (isChecked) {
                            currentTypeFilters.add(t);
                        } else {
                            currentTypeFilters.remove(t);
                        }
                    }
                })
                .setPositiveButton("Confirm", (dialog, which) -> {
                    if (currentTypeFilters.isEmpty()) {
                        // 如果用户只勾选了“全部”或全取消，则视为不过滤
                        currentTypeFilters.clear();
                    }
                    applyFilters();
                    if (!TextUtils.isEmpty(currentKeyword)) {
                        showSearchSheet();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void attachClusterOverlay() {
        if (!isMapLoaded || aMap == null || displayedNotes.isEmpty()) {
            return;
        }
        clearClusterOverlay();
        mClusterOverlay = new ClusterOverlay(aMap, new ArrayList<>(displayedNotes),
                dp2px(getApplicationContext(), clusterRadius),
                getApplicationContext());
        mClusterOverlay.setClusterRenderer(this);
        mClusterOverlay.setOnClusterClickListener(this);
    }

    private void showSearchSheet() {
        if (displayedNotes.isEmpty() || TextUtils.isEmpty(currentKeyword)) {
            hideSearchSheet();
            return;
        }
        searchSheetNotes.clear();
        searchSheetNotes.addAll(displayedNotes);
        tvSearchResultTitle.setText("Search results (" + displayedNotes.size() + ")");
        searchSheetAdapter.notifyDataSetChanged();
        int peek = (int) (getResources().getDisplayMetrics().heightPixels * 0.66f);
        searchSheetBehavior.setPeekHeight(peek);
        searchSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
    }

    private void hideSearchSheet() {
        if (searchSheetBehavior != null) {
            searchSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        }
    }

    private void tweakSearchIconPosition() {
        try {
            int magId = resolveSearchViewId("search_mag_icon");


            int plateId = resolveSearchViewId("search_plate");
            ImageView magIcon = magId != 0 ? searchView.findViewById(magId) : null;
            View searchPlate = plateId != 0 ? searchView.findViewById(plateId) : null;
            if (searchPlate != null) {
                searchPlate.setPadding(0, searchPlate.getPaddingTop(), searchPlate.getPaddingRight(), searchPlate.getPaddingBottom());
            }
            if (magIcon != null) {
                ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) magIcon.getLayoutParams();
                lp.setMarginStart(0);
                lp.leftMargin = 0;
                magIcon.setLayoutParams(lp);
                magIcon.setPadding(0, magIcon.getPaddingTop(), magIcon.getPaddingRight(), magIcon.getPaddingBottom());
            }
        } catch (Exception ignored) {
        }
    }

    private int resolveSearchViewId(String name) {
        int id = searchView.getContext().getResources().getIdentifier(name, "id", getPackageName());
        if (id == 0) {
            id = searchView.getContext().getResources().getIdentifier(name, "id", "androidx.appcompat");
        }
        return id;
    }

    private void initGeocoder() {
        try {
            geocodeSearch = new GeocodeSearch(this);
            geocodeSearch.setOnGeocodeSearchListener(this);
        } catch (Exception e) {
            Log.e(TAG, "Failed to init geocoder", e);
        }
    }

    private void initFuzzySearch() {
        suggestionAdapter = new SimpleCursorAdapter(
                this,
                android.R.layout.simple_list_item_2,
                null,
                new String[]{"suggest_text_1", "suggest_text_2"},
                new int[]{android.R.id.text1, android.R.id.text2},
                CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER
        );
        searchView.setSuggestionsAdapter(suggestionAdapter);
        searchView.setOnSuggestionListener(new SearchView.OnSuggestionListener() {
            @Override
            public boolean onSuggestionSelect(int position) {
                return false;
            }

            @Override
            public boolean onSuggestionClick(int position) {
                handleTipSelection(position);
                return true;
            }
        });
    }

    private void requestInputTips(String keyword) {
        if (TextUtils.isEmpty(keyword)) {
            updateSuggestionCursor(new ArrayList<>());
            return;
        }
        InputtipsQuery query = new InputtipsQuery(keyword, "");
        query.setCityLimit(false); // 允许跨城市模糊匹配
        if (inputtips == null) {
            inputtips = new Inputtips(this, query);
            inputtips.setInputtipsListener(this::onInputTipsReceived);
        } else {
            inputtips.setQuery(query);
        }
        inputtips.requestInputtipsAsyn();
    }

    private void onInputTipsReceived(List<Tip> tips, int rCode) {
        if (rCode != 1000 || tips == null) {
            return;
        }
        currentTips.clear();
        for (Tip tip : tips) {
            // 只保留有坐标的提示，便于直接定位
            if (tip != null && tip.getPoint() != null) {
                currentTips.add(tip);
            }
        }
        updateSuggestionCursor(currentTips);
    }

    private void updateSuggestionCursor(List<Tip> tips) {
        MatrixCursor cursor = new MatrixCursor(SEARCH_SUGGEST_COLUMNS);
        int id = 0;
        for (Tip tip : tips) {
            cursor.addRow(new Object[]{id++, tip.getName(), tip.getAddress()});
        }
        Cursor old = suggestionAdapter.getCursor();
        suggestionAdapter.changeCursor(cursor);
        if (old != null) {
            old.close();
        }
    }

    private void handleTipSelection(int position) {
        if (position < 0 || position >= currentTips.size()) {
            return;
        }
        Tip tip = currentTips.get(position);
        String name = tip.getName();
        searchView.setQuery(name, false);
        applySearchQuery(name);
        if (tip.getPoint() != null && aMap != null) {
            LatLng target = new LatLng(tip.getPoint().getLatitude(), tip.getPoint().getLongitude());
            if (searchMarker != null) {
                searchMarker.remove();
            }
            searchMarker = aMap.addMarker(new com.amap.api.maps.model.MarkerOptions()
                    .position(target)
                    .title(name)
                    .snippet(tip.getAddress()));
            aMap.animateCamera(CameraUpdateFactory.newLatLngZoom(target, 15));
            showSearchSheet();
        }
        searchView.clearFocus();
    }

    private void setupSearchSheet() {
        searchSheetBehavior = BottomSheetBehavior.from(bottomSheetSearch);
        searchSheetBehavior.setHideable(true);
        searchSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        rvSearchResult.setLayoutManager(new LinearLayoutManager(this));
        searchSheetAdapter = new NoteCardAdapter(this, searchSheetNotes);
        rvSearchResult.setAdapter(searchSheetAdapter);
    }

    private void clearClusterOverlay() {
        if (mClusterOverlay != null) {
            mClusterOverlay.onDestroy();
            mClusterOverlay = null;
        }
    }


    /**
     * [唯一] 实现 ClusterRender 接口: 获取聚合点的图标样式
     */
    @Override
    public BitmapDescriptor getBitmapDescriptor(Cluster cluster) {
        if (cluster == null) {
            return null;
        }
        int clusterSize = cluster.getClusterCount();
        if (clusterSize <= 1 && !cluster.getClusterItems().isEmpty()) {
            ClusterItem item = cluster.getClusterItems().get(0);
            if (item instanceof RegionItem) {
                BitmapDescriptor descriptor = getPhotoMarkerDescriptor((RegionItem) item);
                if (descriptor != null) {
                    return descriptor;
                }
            }
        }
        return getClusterPhotoDescriptor(cluster);
    }


    /**
     * [唯一] Helper 方法: 绘制圆形 Bitmap 作为聚合点图标
     */
    private Bitmap drawCircle(int radius, int color) {
        Bitmap bitmap = Bitmap.createBitmap(radius * 2, radius * 2,
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        RectF rectF = new RectF(0, 0, radius * 2, radius * 2);
        paint.setColor(color);
        canvas.drawArc(rectF, 0, 360, true, paint);
        return bitmap;
    }

    /**
     * [唯一] Helper 方法: dp 转 px
     */
    public int dp2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    private BitmapDescriptor getClusterPhotoDescriptor(Cluster cluster) {
        int clusterNum = cluster.getClusterCount();
        int zoomBucket = (int) Math.round(aMap != null ? aMap.getCameraPosition().zoom : 12f);
        String cacheKey = clusterNum + "_" + zoomBucket;
        BitmapDescriptor cached = clusterCircleCache.get(cacheKey);
        if (cached != null) return cached;

        // 取聚合里第一张图片作为背景
        String photoUrl = null;
        if (!cluster.getClusterItems().isEmpty()) {
            ClusterItem item = cluster.getClusterItems().get(0);
            if (item instanceof RegionItem) {
                photoUrl = ((RegionItem) item).getPhotoUrl();
            }
        }

        float scale = getMarkerScale();
        int baseSize = dp2px(getApplicationContext(), 64);
        int size = Math.max(1, (int) (baseSize * scale));
        Bitmap bg = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bg);

        // 先绘制图片方形底（圆角矩形）
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.parseColor("#333333"));
        float radius = dp2px(getApplicationContext(), 12) * scale;
        RectF rect = new RectF(0, 0, size, size);
        canvas.drawRoundRect(rect, radius, radius, paint);

        Bitmap photo = loadBitmapFromUrl(photoUrl);
        if (photo != null) {
            Rect src = new Rect(0, 0, photo.getWidth(), photo.getHeight());
            Rect dst = new Rect(0, 0, size, size);
            Paint imgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            imgPaint.setAntiAlias(true);
            canvas.save();
            Path clipPath = new Path();
            clipPath.addRoundRect(new RectF(dst), radius, radius, Path.Direction.CW);
            canvas.clipPath(clipPath);
            canvas.drawBitmap(photo, src, dst, imgPaint);
            canvas.restore();
        }

        // 叠加数量蓝色气泡
        int badgeRadius = Math.max(8, (int) (dp2px(getApplicationContext(), 12) * scale));
        int badgeCenterX = size - badgeRadius;
        int badgeCenterY = badgeRadius;
        Paint badgePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        badgePaint.setColor(Color.parseColor("#1E88E5"));
        canvas.drawCircle(badgeCenterX, badgeCenterY, badgeRadius, badgePaint);

        // 白色数字
        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(Math.max(10, dp2px(getApplicationContext(), 12) * scale));
        textPaint.setFakeBoldText(true);
        String text = String.valueOf(clusterNum);
        float textWidth = textPaint.measureText(text);
        Paint.FontMetrics fm = textPaint.getFontMetrics();
        float baseline = badgeCenterY + (fm.descent - fm.ascent) / 2f - fm.descent;
        canvas.drawText(text, badgeCenterX - textWidth / 2f, baseline, textPaint);

        BitmapDescriptor descriptor = BitmapDescriptorFactory.fromBitmap(bg);
        clusterCircleCache.put(cacheKey, descriptor);
        return descriptor;
    }

    @Override
    public void onGeocodeSearched(GeocodeResult geocodeResult, int rCode) {
        if (rCode != 1000 || geocodeResult == null || geocodeResult.getGeocodeAddressList() == null
                || geocodeResult.getGeocodeAddressList().isEmpty()) {
            showMsg("Place not found");
            hideSearchSheet();
            return;
        }
        LatLonPoint point = geocodeResult.getGeocodeAddressList().get(0).getLatLonPoint();
        if (point == null || aMap == null) {
            showMsg("Place not found");
            return;
        }
        LatLng latLng = new LatLng(point.getLatitude(), point.getLongitude());
        if (searchMarker != null) {
            searchMarker.remove();
        }
        searchMarker = aMap.addMarker(new com.amap.api.maps.model.MarkerOptions()
                .position(latLng)
                .title("Searched location")
                .snippet(geocodeResult.getGeocodeQuery().getLocationName()));
        aMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
        showSearchSheet();
    }

    @Override
    public void onRegeocodeSearched(RegeocodeResult regeocodeResult, int i) {
        // 不处理逆地理回调
    }

    private BitmapDescriptor getPhotoMarkerDescriptor(RegionItem item) {
        if (item == null) {
            return null;
        }
        String cacheKey = item.getNoteId();
        if (!TextUtils.isEmpty(cacheKey)) {
            BitmapDescriptor cached = markerIconCache.get(cacheKey);
            if (cached != null) {
                return cached;
            }
        }
        Bitmap photoBitmap = loadBitmapFromUrl(item.getPhotoUrl());
        View markerView = markerLayoutInflater.inflate(R.layout.layout_marker_photo, null);
        ImageView imageView = markerView.findViewById(R.id.iv_marker_photo);
        TextView typeView = markerView.findViewById(R.id.tv_marker_type);
        if (photoBitmap != null) {
            imageView.setImageBitmap(photoBitmap);
        } else if (fallbackMarkerBitmap != null) {
            imageView.setImageBitmap(fallbackMarkerBitmap);
        }
        typeView.setText(toEnglishTag(item.getNoteType()));
        Bitmap markerBitmap = createBitmapFromView(markerView);
        float scale = getMarkerScale();
        if (scale != 1f && markerBitmap != null) {
            int w = Math.max(1, (int) (markerBitmap.getWidth() * scale));
            int h = Math.max(1, (int) (markerBitmap.getHeight() * scale));
            markerBitmap = Bitmap.createScaledBitmap(markerBitmap, w, h, true);
        }
        BitmapDescriptor descriptor = BitmapDescriptorFactory.fromBitmap(markerBitmap);
        if (!TextUtils.isEmpty(cacheKey)) {
            markerIconCache.put(cacheKey, descriptor);
        }
        return descriptor;
    }

    private Bitmap createBitmapFromView(View view) {
        int widthSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        int heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        view.measure(widthSpec, heightSpec);
        view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
        Bitmap bitmap = Bitmap.createBitmap(view.getMeasuredWidth(), view.getMeasuredHeight(),
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);
        return bitmap;
    }

    private float getMarkerScale() {
        float zoom = aMap != null && aMap.getCameraPosition() != null
                ? aMap.getCameraPosition().zoom : 12f;
        float factor = (zoom - 10f) / 8f; // zoom 10->0, 18->1
        if (factor < 0f) factor = 0f;
        if (factor > 1f) factor = 1f;
        return 0.7f + factor * 0.45f; // scale 0.7 - 1.15，略放大
    }

    private Bitmap loadBitmapFromUrl(String url) {
        if (TextUtils.isEmpty(url)) {
            return null;
        }
        FutureTarget<Bitmap> futureTarget = null;
        try {
            futureTarget = Glide.with(getApplicationContext())
                    .asBitmap()
                    .load(url)
                    .submit(256, 256);
            return futureTarget.get(6, TimeUnit.SECONDS);
        } catch (Exception e) {
            Log.w(TAG, "Failed to load marker image: " + url, e);
            return null;
        } finally {
            if (futureTarget != null) {
                Glide.with(getApplicationContext()).clear(futureTarget);
            }
        }
    }


    /**
     * 激活定位
     */
    @Override
    public void activate(OnLocationChangedListener onLocationChangedListener) {
        if (mListener == null) {
            mListener = onLocationChangedListener;
        }
        startLocation();
    }

    /**
     * 禁用
     */
    @Override
    public void deactivate() {
        mListener = null;
        if (mLocationClient != null) {
            mLocationClient.stopLocation();
            mLocationClient.onDestroy();
        }
        mLocationClient = null;
    }

    /**
     * [唯一] 方法一：处理地图聚合点 (Cluster) 的点击事件
     * 实现 ClusterClickListener 接口
     */
    @Override
    public void onClick(Marker marker, List<ClusterItem> clusterItems) {

        // [调试点 1] 检查方法是否被调用
        Log.d("ClusterDebug", "onClick(Marker, List) invoked.");

        if (clusterItems == null || clusterItems.isEmpty()) {
            Log.e("ClusterDebug", "Error: clusterItems is null!");
            return;
        }

        // [调试点 2] 检查点击类型
        Log.d("ClusterDebug", "Clicked cluster contains " + clusterItems.size() + " items.");

        // 判断是否是聚合点 (数量大于 1)
        if (clusterItems.size() > 1) {

            // [调试点 3] 确认进入了 "聚合点" 逻辑
                Log.d("ClusterDebug", "Entered multi-item cluster branch.");

            // 1. 转换为可传递的 RegionItem 列表
            ArrayList<RegionItem> notesList = new ArrayList<>();
            for (ClusterItem item : clusterItems) {
                if (item instanceof RegionItem) {
                    notesList.add((RegionItem) item);
                } else {
                    // [调试点 4] 检查类型转换失败
                    Log.e("ClusterDebug", "Error: an element in the cluster is not a RegionItem!");
                }
            }

            if (notesList.isEmpty()) {
                Log.e("ClusterDebug", "Error: notesList empty, conversion failed.");
                Toast.makeText(this, "Cluster data error, unable to parse.", Toast.LENGTH_SHORT).show();
                return;
            }

            // [调试点 5] 检查 RegionItem 是否实现了 Serializable 接口
            try {
                Object testItem = notesList.get(0);
                if (!(testItem instanceof java.io.Serializable)) {
                    // 【最可能的崩溃点 A】
                    Log.e("ClusterDebug", "Fatal: RegionItem.java must implement java.io.Serializable!");
                    Toast.makeText(this, "Crash: RegionItem must implement Serializable", Toast.LENGTH_LONG).show();
                    return; // 阻止应用崩溃
                }
                Log.d("ClusterDebug", "Check passed: RegionItem implements Serializable.");

            } catch (Exception e) {
                Log.e("ClusterDebug", "Exception while checking Serializable: " + e.getMessage());
                return;
            }

            // 2. 启动新的列表详情 Activity
            Log.d("ClusterDebug", "Preparing Intent for ClusterDetailActivity.class");
            Intent intent = new Intent(this, ClusterDetailActivity.class);

            Log.d("ClusterDebug", "Preparing to pass " + notesList.size() + " notes.");
            intent.putExtra(ClusterDetailActivity.EXTRA_CLUSTER_NOTES, notesList);

            try {
                // [调试点 6] 尝试启动 Activity
                Log.d("ClusterDebug", "Calling startActivity(intent)...");
                startActivity(intent);
                Log.d("ClusterDebug", "startActivity() succeeded."); // 如果看到这条，说明 Activity 启动了

            } catch (android.content.ActivityNotFoundException e) {
                // 【最可能的崩溃点 B】
                Log.e("ClusterDebug", "Fatal: ActivityNotFoundException!");
                Log.e("ClusterDebug", "Check AndroidManifest.xml for .ui.ClusterDetailActivity registration");
                Toast.makeText(this, "Crash: Activity not registered in Manifest!", Toast.LENGTH_LONG).show();

            } catch (Exception e) {
                // 【其他崩溃点】
                Log.e("ClusterDebug", "Fatal: Unknown exception during startActivity (possibly serialization): " + e.getMessage());
                Toast.makeText(this, "Crash: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }

        } else if (clusterItems.size() == 1) {

            // [单个笔记] 用户点击了未聚合的单个笔记
            ClusterItem item = clusterItems.get(0);
            if (item instanceof RegionItem) {
                RegionItem note = (RegionItem) item;

                // [修改] 启动 NoteDetailActivity
                Intent intent = new Intent(this, NoteDetailActivity.class);
                // 【关键】将整个笔记对象传递过去
                intent.putExtra(NoteDetailActivity.EXTRA_NOTE_DATA, note);
                startActivity(intent);

            } else {
                // [删除] 临时提示
                // Toast.makeText(this, "点击了单个笔记: " + title, Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * [唯一] 方法二：处理 UI 控件 (FAB 按钮等) 的点击事件
     * 实现 View.OnClickListener 接口
     */
    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.fab_zoom_large) {
            aMap.animateCamera(CameraUpdateFactory.zoomIn());
        } else if (view.getId() == R.id.fab_zoom_small) {
            aMap.animateCamera(CameraUpdateFactory.zoomOut());
        } else if (view.getId() == R.id.fab_location) {
            showTypeFilterDialog();
        }

        // [修改] 新的底部 FAB 按钮的点击逻辑 (ID 已修正)
        else if (view.getId() == R.id.fab_my_location) {
            // “我的位置”按钮
            if (latLng != null) {
                aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 18));
            } else {
                showMsg("Locating...");
            }
        }

        else if (view.getId() == R.id.fab_add_note) {
            // “添加笔记”按钮
            Intent intent = new Intent(MainActivity.this, AddNoteActivity.class);
            if (latLng != null) {
                intent.putExtra("CURRENT_LAT", latLng.latitude);
                intent.putExtra("CURRENT_LNG", latLng.longitude);
            }
            startActivity(intent);
        }

        else if (view.getId() == R.id.fab_user_profile) {
            // “用户资料”按钮
            Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
            startActivity(intent);
        }
    }

    private String toEnglishTag(String raw) {
        if (TextUtils.isEmpty(raw)) return "Recommended";
        String t = raw.trim();
        switch (t) {
            case "种草": return "Recommendation";
            case "攻略": return "Guide";
            case "测评": return "Review";
            case "分享": return "Share";
            case "合集": return "Collection";
            case "教程": return "Tutorial";
            case "开箱": return "Unboxing";
            case "探店": return "Store visit";
            case "推荐": return "Recommended";
            case "美食": return "Food";
            case "风景": return "Scenery";
            default: return t;
        }
    }


}
