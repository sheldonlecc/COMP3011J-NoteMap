package com.noworld.notemap.ui;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;

import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.geocoder.GeocodeQuery;
import com.amap.api.services.geocoder.GeocodeSearch;
import com.amap.api.services.geocoder.RegeocodeQuery;
import com.amap.api.services.geocoder.RegeocodeResult;
import com.amap.api.services.geocoder.GeocodeAddress;
import com.noworld.notemap.R;

import java.util.ArrayList;
import java.util.List;

public class SelectLocationActivity extends AppCompatActivity implements AMap.OnMapClickListener, GeocodeSearch.OnGeocodeSearchListener {

    public static final String EXTRA_LAT = "EXTRA_LAT";
    public static final String EXTRA_LNG = "EXTRA_LNG";
    public static final String EXTRA_ADDRESS = "EXTRA_ADDRESS";

    private MapView mapView;
    private AMap aMap;
    private GeocodeSearch geocodeSearch;
    private LatLng selectedLatLng;
    private String selectedAddress;
    private TextView tvAddress;
    private SearchView searchView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_location);
        Toolbar toolbar = findViewById(R.id.toolbar_select_location);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Select location");
        }

        mapView = findViewById(R.id.map_view_select);
        mapView.onCreate(savedInstanceState);
        tvAddress = findViewById(R.id.tv_selected_address);
        searchView = findViewById(R.id.search_location);
        Button btnConfirm = findViewById(R.id.btn_confirm_location);

        aMap = mapView.getMap();
        aMap.setOnMapClickListener(this);

        double lat = getIntent().getDoubleExtra("CURRENT_LAT", 0);
        double lng = getIntent().getDoubleExtra("CURRENT_LNG", 0);
        if (lat != 0 && lng != 0) {
            LatLng cur = new LatLng(lat, lng);
            aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(cur, 15));
        }

        try {
            geocodeSearch = new GeocodeSearch(this);
            geocodeSearch.setOnGeocodeSearchListener(this);
            initSearch(); // Bind search listener after geocodeSearch is initialized.
        } catch (Exception e) {
            Toast.makeText(this, "Geocoder init failed", Toast.LENGTH_SHORT).show();
        }

        btnConfirm.setOnClickListener(v -> {
            if (selectedLatLng == null) {
                Toast.makeText(this, "Tap the map to choose a location", Toast.LENGTH_SHORT).show();
                return;
            }
            getIntent().putExtra(EXTRA_LAT, selectedLatLng.latitude);
            getIntent().putExtra(EXTRA_LNG, selectedLatLng.longitude);
            getIntent().putExtra(EXTRA_ADDRESS, selectedAddress);
            setResult(RESULT_OK, getIntent());
            finish();
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onMapClick(LatLng latLng) {
        setSelectedLocation(latLng, "Resolving address...");
        if (geocodeSearch != null) {
            RegeocodeQuery query = new RegeocodeQuery(new LatLonPoint(latLng.latitude, latLng.longitude), 200, GeocodeSearch.AMAP);
            geocodeSearch.getFromLocationAsyn(query);
        } else {
            // If geocoding is unavailable, show lat/lng and allow confirmation.
            String addr = String.format("Lat:%.5f Lng:%.5f", latLng.latitude, latLng.longitude);
            setSelectedLocation(latLng, addr);
        }
    }

    @Override
    public void onRegeocodeSearched(RegeocodeResult result, int rCode) {
        if (rCode == 1000 && result != null && result.getRegeocodeAddress() != null) {
            selectedAddress = result.getRegeocodeAddress().getFormatAddress();
            if (selectedLatLng != null) {
                setSelectedLocation(selectedLatLng, selectedAddress);
            } else {
                tvAddress.setText(selectedAddress);
            }
        } else {
            selectedAddress = "";
            tvAddress.setText("Failed to parse address");
        }
    }

    @Override
    public void onGeocodeSearched(com.amap.api.services.geocoder.GeocodeResult geocodeResult, int rCode) {
        if (rCode != 1000 || geocodeResult == null || geocodeResult.getGeocodeAddressList() == null
                || geocodeResult.getGeocodeAddressList().isEmpty()) {
            Toast.makeText(this, "Place not found", Toast.LENGTH_SHORT).show();
            return;
        }
        List<GeocodeAddress> list = geocodeResult.getGeocodeAddressList();
        if (list.size() == 1) {
            LatLonPoint p = list.get(0).getLatLonPoint();
            LatLng latLng = new LatLng(p.getLatitude(), p.getLongitude());
            String addr = list.get(0).getFormatAddress();
            setSelectedLocation(latLng, addr);
            aMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16));
        } else {
            showGeocodeChoice(list);
        }
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

    private void initSearch() {
        if (searchView == null || geocodeSearch == null) return;
        searchView.setIconifiedByDefault(false);
        searchView.setSubmitButtonEnabled(true);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                doForwardGeocode(query);
                searchView.clearFocus();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
    }

    private void doForwardGeocode(String keyword) {
        if (keyword == null || keyword.trim().isEmpty() || geocodeSearch == null) return;
        GeocodeQuery query = new GeocodeQuery(keyword.trim(), "");
        geocodeSearch.getFromLocationNameAsyn(query);
    }

    private void setSelectedLocation(LatLng latLng, String addr) {
        selectedLatLng = latLng;
        selectedAddress = addr;
        if (aMap != null) {
            aMap.clear();
            aMap.addMarker(new MarkerOptions().position(latLng).title("Selected location"));
        }
        if (addr == null || addr.isEmpty()) {
            addr = String.format("Lat:%.5f Lng:%.5f", latLng.latitude, latLng.longitude);
        }
        tvAddress.setText(addr);
    }

    private void showGeocodeChoice(List<GeocodeAddress> list) {
        List<String> addrs = new ArrayList<>();
        for (GeocodeAddress g : list) {
            addrs.add(g.getFormatAddress());
        }
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Choose location")
                .setItems(addrs.toArray(new String[0]), (dialog, which) -> {
                    GeocodeAddress g = list.get(which);
                    LatLonPoint p = g.getLatLonPoint();
                    LatLng latLng = new LatLng(p.getLatitude(), p.getLongitude());
                    setSelectedLocation(latLng, g.getFormatAddress());
                    aMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16));
                })
                .show();
    }
}
