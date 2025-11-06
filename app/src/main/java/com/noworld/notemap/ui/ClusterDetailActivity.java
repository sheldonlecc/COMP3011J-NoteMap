package com.noworld.notemap.ui;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.amap.apis.cluster.ClusterItem;
import com.noworld.notemap.R;
import com.amap.apis.cluster.demo.RegionItem; // 【重要】使用 RegionItem 的实际包名

import java.util.ArrayList;
import java.util.List;

public class ClusterDetailActivity extends AppCompatActivity {

    public static final String EXTRA_CLUSTER_NOTES = "CLUSTER_NOTES_LIST";
    private List<RegionItem> notesList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cluster_detail);

        // 1. 接收数据
        receiveNotesData();

        // 2. 初始化 View 和 Toolbar
        initView();

        // 3. 初始化列表
        if (notesList != null && !notesList.isEmpty()) {
            initRecyclerView();
        } else {
            Toast.makeText(this, "未接收到任何笔记数据，请检查数据传递。", Toast.LENGTH_LONG).show();
        }
    }

    private void initView() {
        Toolbar toolbar = findViewById(R.id.toolbar_cluster_detail);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            // 显示正确的笔记数量
            getSupportActionBar().setTitle("附近笔记 (" + (notesList != null ? notesList.size() : 0) + ")");
        }
    }

    private void receiveNotesData() {
        // [核心] 从 Intent 中获取 Serializable 对象列表
        if (getIntent().hasExtra(EXTRA_CLUSTER_NOTES)) {
            notesList = (ArrayList<RegionItem>) getIntent().getSerializableExtra(EXTRA_CLUSTER_NOTES);
        }
    }

    private void initRecyclerView() {
        RecyclerView rvClusterNotes = findViewById(R.id.rv_cluster_notes);
        rvClusterNotes.setLayoutManager(new LinearLayoutManager(this));

        // 这是一个临时的 Adapter，用于验证功能，后续您需要创建真实的 NoteListAdapter
        rvClusterNotes.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            @NonNull
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull android.view.ViewGroup parent, int viewType) {
                // 占位 View：使用 Android 内置的简单 TextView
                TextView tv = new TextView(parent.getContext());
                tv.setPadding(30, 30, 30, 30);
                return new RecyclerView.ViewHolder(tv) {};
            }

            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
                // 占位内容：展示笔记的标题
                ((TextView)holder.itemView).setText(
                        "笔记 #" + (position + 1) + " - 标题: " + notesList.get(position).getTitle()
                );
            }

            @Override
            public int getItemCount() {
                return notesList.size();
            }
        });
    }

    // 处理 Toolbar 的返回按钮
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}