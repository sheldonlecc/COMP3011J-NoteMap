package com.noworld.notemap.ui;

import android.Manifest;
import android.content.Context; // [新增] 导入 Context
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas; // [新增] 导入 Canvas
import android.graphics.Color;
import android.graphics.Paint; // [新增] 导入 Paint
import android.graphics.RectF; // [新增] 导入 RectF
import android.graphics.drawable.BitmapDrawable; // [新增] 导入 BitmapDrawable
import android.graphics.drawable.Drawable; // [新增] 导入 Drawable
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler; // [新增] 导入 Handler
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;


import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView; // [新增] 导入 SearchView
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.LocationSource;
import com.amap.api.maps.MapView;
import com.amap.api.maps.UiSettings;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.LatLngBounds;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.MyLocationStyle;

import com.amap.api.services.core.PoiItem;
import com.amap.api.services.poisearch.PoiResult;
import com.amap.api.services.poisearch.PoiSearch;
import androidx.appcompat.widget.SearchView;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.services.poisearch.PoiSearchV2;
import com.noworld.notemap.utils.MapUtil;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.noworld.notemap.R;

import org.json.JSONObject;

// [删除] import android.widget.ImageButton; // 已删除


// [新增] 导入点聚合类，使用您提供的 demo 源代码的包名
import com.amap.apis.cluster.ClusterClickListener;
import com.amap.apis.cluster.ClusterItem;
import com.amap.apis.cluster.ClusterOverlay;
import com.amap.apis.cluster.ClusterRender;
// [新增] 导入 RegionItem 所在的 demo 包，假设它在 demo 目录下
import com.amap.apis.cluster.demo.RegionItem;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements AMapLocationListener, LocationSource, View.OnClickListener, PoiSearch.OnPoiSearchListener, ClusterRender, ClusterClickListener {

    private static final String TAG = "MainActivity";

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
    // 声明车辆坐标
    private LatLng carLatLng = null;
    private Marker carMarker = null;
    // 声明是否设置缩放级别
    private boolean isSetZoomLevel = false;

    private android.app.AlertDialog alertDialog;

    // 请求码
    private final int REQUEST_CAMERA_PERMISSION = 101;

    private ActivityResultLauncher<Intent> captureImageLauncher;
    private ActivityResultLauncher<Intent> pickImageLauncher;

    private UiSettings mUiSettings;

    private MapView mp_view;
    // [修改] 保留右上角的 FAB
    private FloatingActionButton fab_zoom_large;
    private FloatingActionButton fab_zoom_small;
    private FloatingActionButton fab_location;

    // [新增] 搜索功能相关的变量
    private SearchView searchView;
    private PoiSearch.Query query;
    private PoiSearch poiSearch;
    private java.util.List<Marker> poiMarkers = new java.util.ArrayList<>();

    // [修改] 新的底部 FAB 按钮变量 (类型从 ImageButton 改为 FloatingActionButton)
    private FloatingActionButton fab_my_location;
    private FloatingActionButton fab_add_note;
    private FloatingActionButton fab_user_profile;

    private androidx.appcompat.widget.Toolbar toolbar_main; // 添加这个变量

    private ClusterOverlay mClusterOverlay;
    private int clusterRadius = 100; // 聚合半径 (dp)
    private Map<Integer, Drawable> mBackDrawAbles = new HashMap<>(); // 缓存不同数量的聚合图标



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();

        setSupportActionBar(toolbar_main);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("");
        }

        requestPermission = registerForActivityResult(new ActivityResultContracts.RequestPermission(), result -> {
            // 权限申请结果
            Log.d(TAG, "权限申请结果: " + result);
            showMsg(result ? "已获取到定位权限" : "权限申请失败");

        });

        // 初始化定位
        initLocation();
        mp_view.onCreate(savedInstanceState);
        // 初始化地图
        initMap();

        Log.d(TAG, "moveCamera: " + latLng);
        // 设置地图默认缩放级别
        aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16));
        Log.d(TAG, "moveEnd!");

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //第二个参数表示此menu的id值，在onOptionsItemSelected方法中通过id值判断是哪个menu被点击了
        menu.add(Menu.NONE, 1, 1, "普通视图");
        menu.add(Menu.NONE, 2, 2, "夜景视图");
        menu.add(Menu.NONE, 3, 3, "卫星视图");
        menu.add(Menu.NONE, 4, 4, "导航视图");
        return true;
    }


    /**
     * 点击实现的操作
     */
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case 1:
                aMap.setMapType(AMap.MAP_TYPE_NORMAL);
                showMsg("切换为普通视图");
                break;
            case 2:
                aMap.setMapType(AMap.MAP_TYPE_NIGHT);
                showMsg("切换为夜景视图");
                break;
            case 3:
                aMap.setMapType(AMap.MAP_TYPE_SATELLITE);
                showMsg("切换为卫星视图");
                break;
            case 4:
                aMap.setMapType(AMap.MAP_TYPE_NAVI);
                showMsg("切换为导航视图");
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
            // 获取到权限
            startLocation();
        } else {
            // 请求定位权限
            requestPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        // 绑定生命周期 onPause
        mp_view.onPause();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // 绑定生命周期 onSaveInstanceState
        mp_view.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // 绑定生命周期 onDestroy
        mp_view.onDestroy();
        // 停止定位
        stopLocation();
        // 销毁定位
        if (mLocationClient != null) {
            mLocationClient.onDestroy();
        }
    }


    private void showMsg(CharSequence llw) {
        Toast.makeText(this, llw, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onLocationChanged(AMapLocation aMapLocation) {
        if (aMapLocation == null) {
            showMsg("定位失败，aMapLocation 为空");
            Log.e(TAG, "定位失败，aMapLocation 为空");
            return;
        }
        // 获取定位结果
        if (aMapLocation.getErrorCode() == 0) {
            // 定位成功
            Log.i(TAG, "定位成功");
//            showMsg("定位成功");
            latLng = new LatLng(aMapLocation.getLatitude(), aMapLocation.getLongitude()); // 获取当前latlng坐标

            // 停止定位
//            stopLocation();
            // 显示地图定位结果
            if (mListener != null) {
                mListener.onLocationChanged(aMapLocation);
            }
            if (!isSetZoomLevel && latLng != null) {
                aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 18));
                Log.d(TAG, "moveEnd!");
                isSetZoomLevel = true;
            }
        } else {
            // 定位失败
            showMsg("定位失败，错误：" + aMapLocation.getErrorInfo());
            Log.e(TAG, "location Error, ErrCode:"
                    + aMapLocation.getErrorCode() + ", errInfo:"
                    + aMapLocation.getErrorInfo());
        }
    }

    private void initView() {
        mp_view = findViewById(R.id.mp_view);
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

        searchView = findViewById(R.id.search_view);

        // [删除] 旧的 fab_picture 长按监听
        // fab_picture.setOnLongClickListener(view -> { ... });

        // [修改] 绑定新的底部导航栏 FAB 按钮 (ID 已修正)
        fab_my_location = findViewById(R.id.fab_my_location);
        fab_add_note = findViewById(R.id.fab_add_note);
        fab_user_profile = findViewById(R.id.fab_user_profile);

        // [修改] 为新 FAB 按钮设置点击监听
        fab_my_location.setOnClickListener(this);
        fab_add_note.setOnClickListener(this);
        fab_user_profile.setOnClickListener(this);

        // [修改] 搜索框的监听逻辑 (这个在上一轮添加过，保持)
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            // 当用户按下“搜索”或回车键时
            @Override
            public boolean onQueryTextSubmit(String keyword) {
                if (keyword != null && !keyword.trim().isEmpty()) {
                    // 我们马上去创建这个 doSearchQuery 方法
                    doSearchQuery(keyword);
                    searchView.clearFocus(); // 隐藏键盘
                } else {
                    showMsg("请输入搜索关键词");
                }
                return true; // 表示事件已被处理
            }

            // 当搜索框内容变化时
            @Override
            public boolean onQueryTextChange(String newText) {
                // 我们暂时不需要处理这个
                return false;
            }
        });
    }

    private void initMarker() {
        try {
            File file = new File(getFilesDir(), "car.json");
            if (file.exists() && file.length() > 0) {
                byte[] bytes = new byte[(int) file.length()];
                FileInputStream fis = new FileInputStream(file);
                fis.read(bytes);
                fis.close();
                JSONObject carLocation = new JSONObject(new String(bytes));
                double latitude = carLocation.getDouble("latitude");
                double longitude = carLocation.getDouble("longitude");
                carLatLng = new LatLng(latitude, longitude);
                MarkerOptions markerOptions = new MarkerOptions();
                markerOptions.position(carLatLng);
                markerOptions.title("车辆位置");
                // 缩小ico_marker图标
                Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ico_marker);
                Bitmap smallBitmap = Bitmap.createScaledBitmap(bitmap, 100, 100, false);
                markerOptions.icon(com.amap.api.maps.model.BitmapDescriptorFactory.fromBitmap(smallBitmap));
                carMarker = aMap.addMarker(markerOptions);
                Log.d(TAG, "initMarker: 车辆标记已初始化");
            }
        } catch (Exception e) {
            e.printStackTrace();
            showMsg("获取车辆位置失败: " + e.getMessage());
        }
    }

    public void initClusterData() {
        // [删除] 原始 demo 中的 mAMap = mMapView.getMap(); 因为我们在 initMap() 中已经初始化

        // [新增] 移除地图点击事件（避免与 onMapClick 动态添加点冲突，如果您需要测试官方 demo 的动态添加，可以保留）
        // aMap.setOnMapClickListener(this);

        // 使用一个新线程来生成和添加大量数据，避免阻塞主线程
        new Thread(() -> {
            List<ClusterItem> items = new ArrayList<>();
            // 随机生成 10000 个点 (示例数据)
            for (int i = 0; i < 10000; i++) {
                double lat = Math.random() + 39.474923;
                double lon = Math.random() + 116.027116;

                LatLng latLng = new LatLng(lat, lon, false);
                // [注意] 这里的 RegionItem 必须是 com.amap.apis.cluster.demo.RegionItem
                // 且该类必须实现 com.amap.apis.cluster.ClusterItem 接口
                RegionItem regionItem = new RegionItem(latLng, "test" + i);
                items.add(regionItem);
            }

            // 在主线程或 HandlerThread 中初始化 ClusterOverlay
            // ClusterOverlay 的初始化内部会处理线程安全
            mClusterOverlay = new ClusterOverlay(aMap, items,
                    dp2px(getApplicationContext(), clusterRadius),
                    getApplicationContext());

            // 设置渲染器和点击监听器
            mClusterOverlay.setClusterRenderer(MainActivity.this);
            mClusterOverlay.setOnClusterClickListener(MainActivity.this);

        }).start();
    }


    /**
     * [新增] 实现 ClusterRender 接口: 获取聚合点的图标样式
     * (直接从官方 Demo 中移植)
     */
    @Override
    public Drawable getDrawAble(int clusterNum) {
        // 聚合图标渲染逻辑 (使用 drawCircle, dp2px 等方法)
        int radius = dp2px(getApplicationContext(), 80);

        // 单个 Marker 的样式 (聚类数为 1)
        if (clusterNum == 1) {
            Drawable bitmapDrawable = mBackDrawAbles.get(1);
            if (bitmapDrawable == null) {
                // [注意] 这里的 R.drawable.icon_openmap_mark 需要您项目中有对应的图片资源
                // 这里暂时用一个默认颜色替代，避免编译错误
                bitmapDrawable = new BitmapDrawable(null, drawCircle(radius/3, Color.GRAY));
                mBackDrawAbles.put(1, bitmapDrawable);
            }
            return bitmapDrawable;
        }

        // 聚合点样式
        else if (clusterNum < 5) {
            Drawable bitmapDrawable = mBackDrawAbles.get(2);
            if (bitmapDrawable == null) {
                bitmapDrawable = new BitmapDrawable(null, drawCircle(radius,
                        Color.argb(159, 210, 154, 6))); // 黄色系
                mBackDrawAbles.put(2, bitmapDrawable);
            }
            return bitmapDrawable;
        } else if (clusterNum < 10) {
            Drawable bitmapDrawable = mBackDrawAbles.get(3);
            if (bitmapDrawable == null) {
                bitmapDrawable = new BitmapDrawable(null, drawCircle(radius,
                        Color.argb(199, 217, 114, 0))); // 橙色系
                mBackDrawAbles.put(3, bitmapDrawable);
            }
            return bitmapDrawable;
        } else {
            Drawable bitmapDrawable = mBackDrawAbles.get(4);
            if (bitmapDrawable == null) {
                bitmapDrawable = new BitmapDrawable(null, drawCircle(radius,
                        Color.argb(235, 215, 66, 2))); // 红色系
                mBackDrawAbles.put(4, bitmapDrawable);
            }
            return bitmapDrawable;
        }
    }

    // =========================================================================
    // [点聚合核心逻辑 - 从官方 Demo 移植]
    // =========================================================================

    /**
     * [新增] 在地图加载完成后，初始化并开始计算点聚合数据
     * 注意: 这是在实现 AMap.OnMapLoadedListener 接口时被回调的
     */



    /**
     * [新增] 实现 ClusterRender 接口: 获取聚合点的图标样式
     * (直接从官方 Demo 中移植)
     */


    /**
     * [新增] 实现 ClusterClickListener 接口: 点击聚合点的处理逻辑
     * (直接从官方 Demo 中移植)
     */
    @Override
    public void onClick(Marker marker, List<ClusterItem> clusterItems) {
        // 点击聚合点后，将地图视野移动到所有聚合点标记的边界
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (ClusterItem clusterItem : clusterItems) {
            builder.include(clusterItem.getPosition());
        }
        LatLngBounds latLngBounds = builder.build();
        // 动画移动，边界留白 0 像素
        aMap.animateCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, 0));
    }

    /**
     * [新增] Helper 方法: 绘制圆形 Bitmap 作为聚合点图标
     * (直接从官方 Demo 中移植)
     */
    private Bitmap drawCircle(int radius, int color) {
        Bitmap bitmap = Bitmap.createBitmap(radius * 2, radius * 2,
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        RectF rectF = new RectF(0, 0, radius * 2, radius * 2);
        paint.setColor(color);
        canvas.drawArc(rectF, 0, 360, true, paint);
        // 在中心绘制数量文本 (如果需要，这里可以简化，只绘制圆)
        return bitmap;
    }

    /**
     * [新增] Helper 方法: dp 转 px
     * (直接从官方 Demo 中移植)
     */
    public int dp2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
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

    @Override
    public void onClick(View view) {
        // [修改] 删除了所有旧 FAB 的 if/else if 逻辑
        // 只保留右上角的 FAB 逻辑
        if (view.getId() == R.id.fab_zoom_large) {
            aMap.animateCamera(CameraUpdateFactory.zoomIn());
        } else if (view.getId() == R.id.fab_zoom_small) {
            aMap.animateCamera(CameraUpdateFactory.zoomOut());
        } else if (view.getId() == R.id.fab_location) {
            aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 18));
        }

        // [修改] 新的底部 FAB 按钮的点击逻辑 (ID 已修正)
        else if (view.getId() == R.id.fab_my_location) {
            // “我的位置”按钮
            // 复用你 fab_location 的逻辑
            if (latLng != null) {
                aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 18));
            } else {
                showMsg("正在定位...");
            }
        }

        else if (view.getId() == R.id.fab_add_note) {
            // “添加笔记”按钮
            // [修改] 启动 AddNoteActivity，并传入当前位置
            Intent intent = new Intent(MainActivity.this, AddNoteActivity.class);
            if (latLng != null) {
                intent.putExtra("CURRENT_LAT", latLng.latitude);
                intent.putExtra("CURRENT_LNG", latLng.longitude);
            }
            startActivity(intent);
        }

        else if (view.getId() == R.id.fab_user_profile) {
            // “用户资料”按钮
            // 这里是后端同学未来需要实现 U1 功能
            // 我们先用一个占位符提示
            Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
            startActivity(intent);
        }
    }



    /**
     * [新增] 执行 POI 搜索
     * @param keyword 搜索关键词
     */
    private void doSearchQuery(String keyword) {
        Log.d(TAG, "doSearchQuery: " + keyword);
        // 第一个参数表示搜索字符串，
        // 第二个参数表示poi搜索类型，传""代表所有类型，
        // 第三个参数表示poi搜索区域，传""代表全国
        query = new PoiSearch.Query(keyword, "", "");
        query.setPageSize(10); // 设置每页最多返回多少条poi
        query.setPageNum(0); // 设置查询第一页

        try {
            // 注意：你的 import 中有 PoiSearchV2，但我们用的是 PoiSearch
            // 确保你的 poiSearch 变量 (line 109) 被正确初始化
            poiSearch = new PoiSearch(this, query);
            poiSearch.setOnPoiSearchListener(this); // 设置回调
            poiSearch.searchPOIAsyn(); // 异步搜索
        } catch (Exception e) {
            e.printStackTrace();
            showMsg("搜索失败");
        }
    }

    /**
     * [新增] POI 搜索成功的回调
     * (这个方法是用来满足 PoiSearch.OnPoiSearchListener 接口的 [line 65])
     */
    @Override
    public void onPoiSearched(PoiResult result, int rCode) {
        Log.d(TAG, "onPoiSearched, rCode: " + rCode);
        if (rCode == 1000) { // 1000 代表成功
            if (result != null && result.getQuery() != null) {
                if (result.getQuery().equals(query)) { // 确认是本次搜索的结果

                    // [清除] 清除上一次的搜索标记
                    for (Marker marker : poiMarkers) {
                        marker.remove();
                    }
                    poiMarkers.clear();

                    java.util.ArrayList<PoiItem> pois = result.getPois();
                    if (pois == null || pois.isEmpty()) {
                        showMsg("没有搜索到相关地点");
                        return;
                    }

                    // [添加] 遍历搜索结果，添加到地图上
                    for (int i = 0; i < pois.size(); i++) {
                        PoiItem poiItem = pois.get(i);

                        // 使用你已有的 MapUtil 工具类
                        LatLng latLng = MapUtil.convertToLatLng(poiItem.getLatLonPoint());

                        Marker marker = aMap.addMarker(new MarkerOptions()
                                .position(latLng)
                                .title(poiItem.getTitle())
                                .snippet(poiItem.getSnippet()));

                        poiMarkers.add(marker); // 保存起来，方便下次清除
                    }

                    // [移动] 将地图视野移动到第一个搜索结果
                    if (!pois.isEmpty()) {
                        PoiItem firstPoi = pois.get(0);
                        aMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                                MapUtil.convertToLatLng(firstPoi.getLatLonPoint()), 15));
                    }
                }
            } else {
                showMsg("没有搜索到相关地点");
            }
        } else {
            showMsg("搜索失败，错误码: " + rCode);
        }
    }

    /**
     * [新增] POI 搜索单个点详情的回调
     * (这个方法也是用来满足 PoiSearch.OnPoiSearchListener 接口的 [line 65])
     */
    @Override
    public void onPoiItemSearched(PoiItem poiItem, int i) {
        // 这个方法是获取单个POI的详细信息时回调的，我们这里暂时用不到
    }
}

