package com.noworld.notemap.data;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amap.apis.cluster.demo.RegionItem;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Firestore / Storage 数据访问封装。
 */
public class FirebaseNoteRepository {

    private static final String COLLECTION_NOTES = "notes";
    private static final String STORAGE_FOLDER = "note_images";

    private static FirebaseNoteRepository INSTANCE;

    private final FirebaseFirestore firestore;
    private final FirebaseStorage storage;
    private final CollectionReference notesCollection;

    private FirebaseNoteRepository() {
        firestore = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        notesCollection = firestore.collection(COLLECTION_NOTES);
    }

    public static FirebaseNoteRepository getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new FirebaseNoteRepository();
        }
        return INSTANCE;
    }

    public interface NotesSnapshotListener {
        void onNotesChanged(List<RegionItem> notes);

        void onError(@NonNull Exception exception);
    }

    public ListenerRegistration observeNotes(@NonNull NotesSnapshotListener listener) {
        return notesCollection
                .orderBy(MapNote.FIELD_TIMESTAMP, Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        listener.onError(error);
                        return;
                    }
                    if (value == null || value.isEmpty()) {
                        listener.onNotesChanged(new ArrayList<>());
                        return;
                    }
                    List<RegionItem> regionItems = new ArrayList<>();
                    value.getDocuments().forEach(documentSnapshot -> {
                        MapNote note = MapNote.fromSnapshot(documentSnapshot);
                        regionItems.add(note.toRegionItem());
                    });
                    NoteLikeManager.getInstance().applyLikedState(regionItems);
                    listener.onNotesChanged(regionItems);
                });
    }

    public Task<Void> publishNote(@NonNull MapNote note, @NonNull String documentId) {
        note.setId(documentId);
        return notesCollection.document(documentId).set(note.toFirestoreMap());
    }

    public Task<String> uploadImage(@NonNull Uri imageUri) {
        String fileName = UUID.randomUUID() + ".jpg";
        StorageReference reference = storage.getReference()
                .child(STORAGE_FOLDER)
                .child(fileName);
        UploadTask uploadTask = reference.putFile(imageUri);
        return uploadTask.continueWithTask(task -> {
            if (!task.isSuccessful()) {
                throw task.getException() != null ? task.getException() : new IllegalStateException("上传失败");
            }
            return reference.getDownloadUrl();
        }).continueWith(task -> {
            if (!task.isSuccessful()) {
                throw task.getException() != null ? task.getException() : new IllegalStateException("获取下载地址失败");
            }
            return task.getResult().toString();
        });
    }

    public String generateNoteId() {
        return notesCollection.document().getId();
    }
}

