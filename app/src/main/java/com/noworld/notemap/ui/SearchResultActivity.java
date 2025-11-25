package com.noworld.notemap.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.apis.cluster.demo.RegionItem;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.noworld.notemap.R;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 搜索结果页：上半屏展示地图位置，下方 BottomSheet 展示相关笔记列表。
 */
public class SearchResultActivity extends AppCompatActivity {

    public static final String EXTRA_SEARCH_NOTES = "EXTRA_SEARCH_NOTES";

    private MapView mapView;
    private AMap aMap;
    private RecyclerView rvNotes;
    private BottomSheetBehavior<RecyclerView> bottomSheetBehavior;
    private List<RegionItem> notes = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_result);

        Serializable data = getIntent().getSerializableExtra(EXTRA_SEARCH_NOTES);
        if (data instanceof ArrayList) {
            //noinspection unchecked
            notes = (ArrayList<RegionItem>) data;
        }
        if (notes == null || notes.isEmpty()) {
            Toast.makeText(this, "未找到相关笔记", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        Toolbar toolbar = findViewById(R.id.toolbar_search_result);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("搜索结果 (" + notes.size() + ")");
        }

        mapView = findViewById(R.id.map_view_search);
        rvNotes = findViewById(R.id.rv_search_notes);

        mapView.onCreate(savedInstanceState);
        initMap();
        initList();
    }

    private void initMap() {
        aMap = mapView.getMap();
        aMap.getUiSettings().setZoomControlsEnabled(false);
        aMap.getUiSettings().setCompassEnabled(true);
        aMap.getUiSettings().setScaleControlsEnabled(true);
        addMarkersAndMoveCamera();
    }

    private void addMarkersAndMoveCamera() {
        LatLng center = null;
        int count = 0;
        for (RegionItem item : notes) {
            if (item == null || item.getPosition() == null) continue;
            LatLng latLng = item.getPosition();
            aMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title(!TextUtils.isEmpty(item.getTitle()) ? item.getTitle() : "笔记")
                    .snippet(item.getLocationName()));
            center = center == null ? latLng : new LatLng(
                    (center.latitude * count + latLng.latitude) / (count + 1),
                    (center.longitude * count + latLng.longitude) / (count + 1)
            );
            count++;
        }
        if (center != null) {
            aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(center, 14));
        }
    }

    private void initList() {
        rvNotes.setLayoutManager(new LinearLayoutManager(this));
        rvNotes.setAdapter(new NoteCardAdapter(this, notes));
        bottomSheetBehavior = BottomSheetBehavior.from(rvNotes);
        bottomSheetBehavior.setPeekHeight(getResources().getDimensionPixelSize(R.dimen.search_bottom_peek_height));
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
