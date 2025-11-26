package com.noworld.notemap.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.noworld.notemap.R;
import com.noworld.notemap.data.MapNote;

import java.util.ArrayList;
import java.util.List;

public class MyNotesAdapter extends RecyclerView.Adapter<MyNotesAdapter.NoteVH> {

    public interface OnUnlikeListener {
        void onUnlike(MapNote note);
    }

    private final List<MapNote> data = new ArrayList<>();
    private final boolean showUnlike;
    private final OnUnlikeListener onUnlikeListener;

    public MyNotesAdapter() {
        this(false, null);
    }

    public MyNotesAdapter(boolean showUnlike, OnUnlikeListener listener) {
        this.showUnlike = showUnlike;
        this.onUnlikeListener = listener;
    }

    public void submit(List<MapNote> notes) {
        data.clear();
        if (notes != null) {
            data.addAll(notes);
        }
        notifyDataSetChanged();
    }

    public void removeById(String id) {
        if (id == null) return;
        for (int i = 0; i < data.size(); i++) {
            MapNote n = data.get(i);
            if (id.equals(n.getId())) {
                data.remove(i);
                notifyItemRemoved(i);
                return;
            }
        }
    }

    @NonNull
    @Override
    public NoteVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_my_note, parent, false);
        return new NoteVH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteVH holder, int position) {
        MapNote note = data.get(position);
        holder.tvTitle.setText(note.getTitle());
        holder.tvDesc.setText(note.getDescription());
        holder.tvLocation.setText(note.getLocationName());
        holder.tvType.setText(note.getType());
        String cover = note.getCoverUrl();
        if (cover != null) {
            holder.ivCover.setVisibility(View.VISIBLE);
            Glide.with(holder.ivCover.getContext()).load(cover).into(holder.ivCover);
        } else {
            holder.ivCover.setVisibility(View.GONE);
        }

        if (showUnlike && onUnlikeListener != null) {
            holder.btnUnlike.setVisibility(View.VISIBLE);
            holder.btnUnlike.setOnClickListener(v -> onUnlikeListener.onUnlike(note));
        } else {
            holder.btnUnlike.setVisibility(View.GONE);
            holder.btnUnlike.setOnClickListener(null);
        }
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class NoteVH extends RecyclerView.ViewHolder {
        TextView tvTitle;
        TextView tvDesc;
        TextView tvLocation;
        TextView tvType;
        ImageView ivCover;
        Button btnUnlike;

        NoteVH(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_note_title);
            tvDesc = itemView.findViewById(R.id.tv_note_desc);
            tvLocation = itemView.findViewById(R.id.tv_note_location);
            tvType = itemView.findViewById(R.id.tv_note_type);
            ivCover = itemView.findViewById(R.id.iv_note_cover);
            btnUnlike = itemView.findViewById(R.id.btn_unlike);
        }
    }
}
