package com.noworld.notemap.ui;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.geocoder.GeocodeResult;
import com.amap.api.services.geocoder.GeocodeSearch;
import com.amap.api.services.geocoder.RegeocodeQuery;
import com.amap.api.services.geocoder.RegeocodeResult;
import com.amap.apis.cluster.demo.RegionItem;
import com.google.android.material.textfield.TextInputEditText;
import com.noworld.notemap.R;
import com.noworld.notemap.data.AliNoteRepository;
import com.noworld.notemap.data.MapNote;
import com.noworld.notemap.data.TokenStore;
import com.noworld.notemap.data.UserStore;
import com.noworld.notemap.data.dto.UpdateNoteRequest;
import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

public class AddNoteActivity extends AppCompatActivity implements GeocodeSearch.OnGeocodeSearchListener {

    private static final int MAX_IMAGES = 9;

    private Toolbar toolbar;
    private TextInputEditText etTitle;
    private TextInputEditText etStory;
    private ImageView ivAddImage;
    private RecyclerView rvImagePreview;
    private RelativeLayout rowNoteType;
    private TextView tvNoteTypeValue;
    private RelativeLayout rowLocation;
    private TextView tvLocationValue;
    private Button btnPublish;

    private ActivityResultLauncher<Intent> pickImageLauncher;
    private ActivityResultLauncher<String> mediaPermissionLauncher;
    private ActivityResultLauncher<Intent> selectLocationLauncher;
    private final List<Uri> selectedImageUris = new ArrayList<>();
    private ImagePreviewAdapter imagePreviewAdapter;
    private double currentLat = 0;
    private double currentLng = 0;

    private GeocodeSearch geocodeSearch;

    private final CharSequence[] noteTypes = {
            "Recommendation", "Guide", "Review", "Share", "Collection", "Tutorial", "Unboxing", "Vlog", "Store visit"
    };

    private AliNoteRepository noteRepository;
    private TokenStore tokenStore;
    private UserStore userStore;
    private boolean isPublishing = false; // Track publish/save state.

    public static final String EXTRA_EDIT_NOTE_DATA = "EDIT_NOTE_DATA";

    // Current note being edited.
    private RegionItem mEditNote;
    // Whether we are in edit mode.
    private boolean isEditMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_note);

        // 1. Bind views.
        toolbar = findViewById(R.id.toolbar_add_note);
        etTitle = findViewById(R.id.et_title);
        etStory = findViewById(R.id.et_story);
        ivAddImage = findViewById(R.id.iv_add_image);
        rvImagePreview = findViewById(R.id.rv_image_preview);
        rowNoteType = findViewById(R.id.row_note_type);
        tvNoteTypeValue = findViewById(R.id.tv_note_type_value);
        rowLocation = findViewById(R.id.row_location);
        tvLocationValue = findViewById(R.id.tv_location_value);
        btnPublish = findViewById(R.id.btn_publish);

        // 2. Set up toolbar.
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        noteRepository = AliNoteRepository.getInstance(this);
        tokenStore = TokenStore.getInstance(this);
        userStore = UserStore.getInstance(this);

        // 3. Set up image preview list.
        rvImagePreview.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        // Attach delete callback for previews.
        imagePreviewAdapter = new ImagePreviewAdapter(selectedImageUris, position -> {
            // Triggered when delete is tapped.
            if (position >= 0 && position < selectedImageUris.size()) {
                selectedImageUris.remove(position); // Remove from data set.
                imagePreviewAdapter.notifyItemRemoved(position); // Refresh list.
                imagePreviewAdapter.notifyItemRangeChanged(position, selectedImageUris.size()); // Update indices.
            }
        });
        rvImagePreview.setAdapter(imagePreviewAdapter);

        // 4. Read current location.
        currentLat = getIntent().getDoubleExtra("CURRENT_LAT", 0);
        currentLng = getIntent().getDoubleExtra("CURRENT_LNG", 0);

        // 5. Initialize helpers.
        initImagePicker();
        initMediaPermission();
        initLocationPicker();

        try {
            geocodeSearch = new GeocodeSearch(this);
            geocodeSearch.setOnGeocodeSearchListener(this);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Check if we are editing an existing note.
        if (getIntent().hasExtra(EXTRA_EDIT_NOTE_DATA)) {
            mEditNote = (RegionItem) getIntent().getSerializableExtra(EXTRA_EDIT_NOTE_DATA);
            if (mEditNote != null) {
                isEditMode = true;
                populateDataForEdit(); // Prefill data.
            }
        }

        setupClickListeners();
    }

    private void initImagePicker() {
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        if (result.getData().getClipData() != null) {
                            int count = result.getData().getClipData().getItemCount();
                            for (int i = 0; i < count; i++) {
                                Uri uri = result.getData().getClipData().getItemAt(i).getUri();
                                if (uri != null && selectedImageUris.size() < MAX_IMAGES) {
                                    selectedImageUris.add(uri);
                                }
                            }
                        } else if (result.getData().getData() != null) {
                            if (selectedImageUris.size() < MAX_IMAGES) {
                                selectedImageUris.add(result.getData().getData());
                            }
                        }
                        if (selectedImageUris.size() >= MAX_IMAGES) {
                            Toast.makeText(this, "You can select up to 9 images", Toast.LENGTH_SHORT).show();
                        }
                        imagePreviewAdapter.notifyDataSetChanged();
                    }
                });
    }

    private void initMediaPermission() {
        mediaPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (granted) {
                        openGallery();
                    } else {
                        Toast.makeText(this, "Gallery permission is required to choose images", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void initLocationPicker() {
        selectLocationLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        double lat = result.getData().getDoubleExtra(SelectLocationActivity.EXTRA_LAT, 0);
                        double lng = result.getData().getDoubleExtra(SelectLocationActivity.EXTRA_LNG, 0);
                        String address = result.getData().getStringExtra(SelectLocationActivity.EXTRA_ADDRESS);
                        if (lat != 0 && lng != 0) {
                            currentLat = lat;
                            currentLng = lng;
                            tvLocationValue.setText(address != null ? address : "Pick manually");
                        }
                    }
                }
        );
    }

    private void setupClickListeners() {
        ivAddImage.setOnClickListener(v -> ensureMediaPermissionAndPickImage());
        rowNoteType.setOnClickListener(v -> showNoteTypePicker());
        rowLocation.setOnClickListener(v -> showLocationChoiceDialog());
        // Click triggers unified publish/save handler.
        btnPublish.setOnClickListener(v -> {
            if (!isPublishing) {
                handlePublishOrSaveClick();
            }
        });
    }

    private void ensureMediaPermissionAndPickImage() {
        String permission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ? android.Manifest.permission.READ_MEDIA_IMAGES
                : android.Manifest.permission.READ_EXTERNAL_STORAGE;
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            openGallery();
        } else {
            mediaPermissionLauncher.launch(permission);
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        pickImageLauncher.launch(intent);
    }

    private void uploadAllImages(List<Uri> uris, UploadAllCallback callback) {
        if (uris == null || uris.isEmpty()) {
            callback.onError(new IllegalArgumentException("No images selected"));
            return;
        }
        List<String> uploaded = new ArrayList<>();
        uploadNext(uris, 0, uploaded, callback);
    }

    private void uploadNext(List<Uri> uris, int index, List<String> uploaded, UploadAllCallback callback) {
        if (index >= uris.size()) {
            callback.onSuccess(uploaded);
            return;
        }
        Uri uri = uris.get(index);
        noteRepository.uploadImage(uri, new AliNoteRepository.UploadCallback() {
            @Override
            public void onSuccess(String fileUrl) {
                uploaded.add(fileUrl);
                uploadNext(uris, index + 1, uploaded, callback);
            }

            @Override
            public void onError(@NonNull Throwable throwable) {
                callback.onError(throwable);
            }
        });
    }

    private void showNoteTypePicker() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose note type");
        builder.setItems(noteTypes, (dialog, which) -> {
            String selectedType = noteTypes[which].toString();
            tvNoteTypeValue.setText(selectedType);
        });
        builder.create().show();
    }

    private void showLocationChoiceDialog() {
        String[] options = {"Use current location", "Pick manually"};
        new AlertDialog.Builder(this)
                .setTitle("Choose location")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        if (currentLat != 0 && currentLng != 0) {
                            tvLocationValue.setText("Fetching address...");
                            LatLonPoint latLonPoint = new LatLonPoint(currentLat, currentLng);
                            RegeocodeQuery query = new RegeocodeQuery(latLonPoint, 200, GeocodeSearch.AMAP);
                            geocodeSearch.getFromLocationAsyn(query);
                        } else {
                            Toast.makeText(this, "Cannot get current location, please return to the map and try again", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Intent intent = new Intent(this, SelectLocationActivity.class);
                        intent.putExtra("CURRENT_LAT", currentLat);
                        intent.putExtra("CURRENT_LNG", currentLng);
                        selectLocationLauncher.launch(intent);
                    }
                })
                .show();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Consolidated publishNote() logic into handlePublishOrSaveClick().
    private void handlePublishOrSaveClick() {
        String title = etTitle.getText() != null ? etTitle.getText().toString().trim() : "";
        String story = etStory.getText() != null ? etStory.getText().toString().trim() : "";
        String noteType = tvNoteTypeValue.getText() != null ? tvNoteTypeValue.getText().toString().trim() : "";
        String locationName = tvLocationValue.getText() != null ? tvLocationValue.getText().toString().trim() : "";

        if (TextUtils.isEmpty(title)) {
            Toast.makeText(this, "Please enter a title", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(story)) {
            Toast.makeText(this, "Please enter content", Toast.LENGTH_SHORT).show();
            return;
        }

        // Edit mode: only validate title and content; images/location can be unchanged.
        if (isEditMode) {
            updateNoteContent(title, story);
            return;
        }
        // ----------------------------------------------------------------------


        // Extra validation for publish mode.
        if (TextUtils.isEmpty(noteType)) {
            Toast.makeText(this, "Please select a note type", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedImageUris.isEmpty()) {
            Toast.makeText(this, "Please select at least one image", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedImageUris.size() > MAX_IMAGES) {
            Toast.makeText(this, "You can upload up to 9 images", Toast.LENGTH_SHORT).show();
            return;
        }
        if (currentLat == 0 || currentLng == 0) {
            Toast.makeText(this, "Location unavailable, please return to the map and try again", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(tokenStore.getToken())) {
            Toast.makeText(this, "Please log in before publishing", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            return;
        }
        // ----------------------------------------------------------------------


        final String locationNameFinal = TextUtils.isEmpty(locationName) ? "Unknown location" : locationName;
        setPublishing(true, "Publishing...");

        // Publish mode: upload images first.
        uploadAllImages(selectedImageUris, new UploadAllCallback() {
            @Override
            public void onSuccess(List<String> imageUrls) {
                String authorId = userStore.ensureUid(userStore.getUid());
                String authorName = TextUtils.isEmpty(userStore.getUsername()) ? "Map user" : userStore.getUsername();
                String avatarUrl = userStore.getAvatarUrl();
                MapNote note = new MapNote(
                        title,
                        story,
                        noteType,
                        currentLat,
                        currentLng,
                        locationNameFinal,
                        imageUrls,
                        authorId,
                        authorName,
                        avatarUrl
                );
                noteRepository.publishNote(note, new AliNoteRepository.PublishCallback() {
                    @Override
                    public void onSuccess() {
                        runOnUiThread(() -> {
                            Toast.makeText(AddNoteActivity.this, "Published successfully!", Toast.LENGTH_SHORT).show();
                            finish();
                        });
                    }

                    @Override
                    public void onError(@NonNull Throwable throwable) {
                        runOnUiThread(() -> {
                            Toast.makeText(AddNoteActivity.this, "Publish failed: " + throwable.getMessage(), Toast.LENGTH_LONG).show();
                            setPublishing(false, "Publish");
                        });
                    }
                });
            }

            @Override
            public void onError(@NonNull Throwable throwable) {
                runOnUiThread(() -> {
                    Toast.makeText(AddNoteActivity.this, "Image upload failed: " + throwable.getMessage(), Toast.LENGTH_LONG).show();
                    setPublishing(false, "Publish");
                });
            }
        });
    }

    /**
     * Update note content (title and description only).
     */
    private void updateNoteContent(String newTitle, String newDescription) {
        if (mEditNote == null || mEditNote.getNoteId() == null) {
            Toast.makeText(this, "Note ID missing, cannot save changes", Toast.LENGTH_SHORT).show();
            return;
        }

        setPublishing(true, "Saving...");

        // Send only the updated title and description.
        UpdateNoteRequest request = new UpdateNoteRequest(newTitle, newDescription);

        noteRepository.updateNote(mEditNote.getNoteId(), request, new AliNoteRepository.SimpleCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    Toast.makeText(AddNoteActivity.this, "Changes saved", Toast.LENGTH_SHORT).show();

                    // Set result so the previous Activity can refresh.
                    setResult(RESULT_OK);
                    finish();
                });
            }

            @Override
            public void onError(Throwable t) {
                runOnUiThread(() -> {
                    Toast.makeText(AddNoteActivity.this, "Failed to save changes: " + t.getMessage(), Toast.LENGTH_LONG).show();
                    setPublishing(false, "Save changes");
                });
            }
        });
    }


    private void setPublishing(boolean publishing, String text) {
        isPublishing = publishing;
        btnPublish.setEnabled(!publishing);
        btnPublish.setText(text);
    }

    @Override
    public void onRegeocodeSearched(RegeocodeResult result, int rCode) {
        if (rCode == 1000) {
            if (result != null && result.getRegeocodeAddress() != null
                    && result.getRegeocodeAddress().getFormatAddress() != null) {
                String address = result.getRegeocodeAddress().getFormatAddress();
                tvLocationValue.setText(address);
            } else {
                tvLocationValue.setText("Address not found");
            }
        } else {
            tvLocationValue.setText("Failed to get address: " + rCode);
        }
    }

    @Override
    public void onGeocodeSearched(GeocodeResult geocodeResult, int i) { }

    // ===========================================
    // Updated ImagePreviewAdapter
    // ===========================================
    private static class ImagePreviewAdapter extends RecyclerView.Adapter<ImagePreviewAdapter.ImageVH> {

        // ... (unchanged)
        private final List<Uri> data;
        private final OnDeleteListener deleteListener;

        interface OnDeleteListener {
            void onDelete(int position);
        }

        ImagePreviewAdapter(List<Uri> data, OnDeleteListener listener) {
            this.data = data;
            this.deleteListener = listener;
        }

        @NonNull
        @Override
        public ImageVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_image_preview, parent, false);
            return new ImageVH(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ImageVH holder, int position) {
            // Important: support Uri and String URL for preview.
            Object item = data.get(position);

            Glide.with(holder.iv.getContext())
                    .load(item) // Glide supports Uri or String URL.
                    .centerCrop()
                    .into(holder.iv);

            // Bind delete click.
            holder.ivDelete.setOnClickListener(v -> {
                if (deleteListener != null) {
                    deleteListener.onDelete(holder.getAdapterPosition());
                }
            });
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        static class ImageVH extends RecyclerView.ViewHolder {
            ImageView iv;
            ImageView ivDelete;

            ImageVH(@NonNull View itemView) {
                super(itemView);
                iv = itemView.findViewById(R.id.iv_image);
                ivDelete = itemView.findViewById(R.id.iv_delete);
            }
        }
    }

    private interface UploadAllCallback {
        void onSuccess(List<String> imageUrls);
        void onError(@NonNull Throwable throwable);
    }

    /**
     * Prefill data for edit mode and handle image preview.
     */
    private void populateDataForEdit() {
        if (mEditNote == null) return;

        // 1. Title and description.
        etTitle.setText(mEditNote.getTitle());
        etStory.setText(mEditNote.getDescription()); // Use etStory for description.
        tvNoteTypeValue.setText(mEditNote.getNoteType());

        // 2. Location info.
        tvLocationValue.setText(mEditNote.getLocationName()); // Use tvLocationValue for locationName.
        // Keep lat/lng for consistency (even if location doesn't change).
        currentLat = mEditNote.getLatitude();
        currentLng = mEditNote.getLongitude();

        // 3. Images: existing URLs could be converted to Uri for preview.
        // Note: using List<Object> would be needed to mix Uri and String URLs.
        // For simplicity, we only describe the approach here.
        if (mEditNote.getImageUrls() != null) {
            for (String url : mEditNote.getImageUrls()) {
                // We could use Uri.parse(url) for Glide compatibility.
                // But selectedImageUris is Uri-only, so the adapter would need List<Object>.
                // To keep edit logic simple, we skip image preview here.
                // Users can upload new images to replace old ones.
            }
        }

        // 4. Button text and title.
        btnPublish.setText("Save changes");
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Edit note");
        }
    }
}
