package com.noworld.notemap.ui;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.RecyclerView;

// [新增] 导入瀑布流布局
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.noworld.notemap.R;
import com.amap.apis.cluster.demo.RegionItem;
import java.util.ArrayList;
import java.util.List;

public class ClusterDetailActivity extends AppCompatActivity {

    public static final String EXTRA_CLUSTER_NOTES = "CLUSTER_NOTES_LIST";
    private List<RegionItem> notesList;
    private RecyclerView rvClusterNotes;
    private NoteCardAdapter adapter; // [新增] 使用新的 Adapter

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cluster_detail);

        receiveNotesData();
        initView();

        if (notesList != null && !notesList.isEmpty()) {
            initRecyclerView();
        } else {
            Toast.makeText(this, "No note data received.", Toast.LENGTH_LONG).show();
        }
    }

    private void initView() {
        Toolbar toolbar = findViewById(R.id.toolbar_cluster_detail);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Nearby notes (" + (notesList != null ? notesList.size() : 0) + ")");
        }
        rvClusterNotes = findViewById(R.id.rv_cluster_notes);
    }

    private void receiveNotesData() {
        if (getIntent().hasExtra(EXTRA_CLUSTER_NOTES)) {
            notesList = (ArrayList<RegionItem>) getIntent().getSerializableExtra(EXTRA_CLUSTER_NOTES);
        }
    }

    private void initRecyclerView() {
        // [修改] 1. 使用瀑布流布局，设置为 2 列
        StaggeredGridLayoutManager layoutManager =
                new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
        rvClusterNotes.setLayoutManager(layoutManager);

        // [修改] 2. 使用新的 NoteCardAdapter
        adapter = new NoteCardAdapter(this, notesList);
        rvClusterNotes.setAdapter(adapter);
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
