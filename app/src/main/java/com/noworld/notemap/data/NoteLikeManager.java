package com.noworld.notemap.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amap.apis.cluster.demo.RegionItem;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 负责“点赞”业务逻辑：缓存当前用户点赞过的笔记，提供点赞/取消点赞的事务操作。
 */
public class NoteLikeManager {

    private static final String FIELD_LIKED_NOTE_IDS = "liked_note_ids";

    private static NoteLikeManager INSTANCE;

    private final FirebaseAuth auth;
    private final FirebaseFirestore firestore;
    private final Set<String> likedNoteIds = new HashSet<>();
    private ListenerRegistration likedListener;
    private final FirebaseAuth.AuthStateListener authStateListener = firebaseAuth -> attachLikedListener();

    private NoteLikeManager() {
        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        auth.addAuthStateListener(authStateListener);
        attachLikedListener();
    }

    public static synchronized NoteLikeManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new NoteLikeManager();
        }
        return INSTANCE;
    }

    private void attachLikedListener() {
        if (likedListener != null) {
            likedListener.remove();
            likedListener = null;
        }
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            likedNoteIds.clear();
            return;
        }
        likedListener = firestore.collection("users")
                .document(user.getUid())
                .addSnapshotListener((snapshot, error) -> {
                    if (snapshot != null && snapshot.exists()) {
                        List<String> likedList = (List<String>) snapshot.get(FIELD_LIKED_NOTE_IDS);
                        synchronized (likedNoteIds) {
                            likedNoteIds.clear();
                            if (likedList != null) {
                                likedNoteIds.addAll(likedList);
                            }
                        }
                    } else {
                        likedNoteIds.clear();
                    }
                });
    }

    public void applyLikedState(@Nullable List<RegionItem> notes) {
        if (notes == null) {
            return;
        }
        synchronized (likedNoteIds) {
            for (RegionItem item : notes) {
                if (item == null) continue;
                item.setLikedByCurrentUser(likedNoteIds.contains(item.getNoteId()));
            }
        }
    }

    public void toggleLike(@NonNull RegionItem note, @NonNull LikeCallback callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onRequireLogin();
            return;
        }
        DocumentReference noteRef = firestore.collection("notes").document(note.getNoteId());
        DocumentReference userRef = firestore.collection("users").document(user.getUid());
        firestore.runTransaction(transaction -> {
            DocumentSnapshot noteSnapshot = transaction.get(noteRef);
            DocumentSnapshot userSnapshot = transaction.get(userRef);

            long likeCount = 0;
            if (noteSnapshot.exists()) {
                Long count = noteSnapshot.getLong(MapNote.FIELD_LIKE_COUNT);
                likeCount = count != null ? count : 0;
            }

            List<String> likedList = userSnapshot.exists()
                    ? (List<String>) userSnapshot.get(FIELD_LIKED_NOTE_IDS)
                    : new ArrayList<>();

            boolean alreadyLiked = likedList != null && likedList.contains(note.getNoteId());

            Map<String, Object> baseUserInfo = new HashMap<>();
            baseUserInfo.put("uid", user.getUid());
            baseUserInfo.put("email", user.getEmail());
            transaction.set(userRef, baseUserInfo, SetOptions.merge());

            if (alreadyLiked) {
                likeCount = Math.max(0, likeCount - 1);
                transaction.update(userRef, FIELD_LIKED_NOTE_IDS, FieldValue.arrayRemove(note.getNoteId()));
            } else {
                likeCount = likeCount + 1;
                transaction.update(userRef, FIELD_LIKED_NOTE_IDS, FieldValue.arrayUnion(note.getNoteId()));
            }
            transaction.update(noteRef, MapNote.FIELD_LIKE_COUNT, likeCount);

            return new LikeResult(!alreadyLiked, (int) likeCount);
        }).addOnSuccessListener(result -> {
            synchronized (likedNoteIds) {
                if (result.isLiked()) {
                    likedNoteIds.add(note.getNoteId());
                } else {
                    likedNoteIds.remove(note.getNoteId());
                }
            }
            callback.onResult(result.isLiked(), result.getLikeCount());
        }).addOnFailureListener(callback::onError);
    }

    public interface LikeCallback {
        void onResult(boolean liked, int likeCount);
        void onRequireLogin();
        void onError(@NonNull Exception e);
    }

    private static class LikeResult {
        private final boolean liked;
        private final int likeCount;

        LikeResult(boolean liked, int likeCount) {
            this.liked = liked;
            this.likeCount = likeCount;
        }

        boolean isLiked() {
            return liked;
        }

        int getLikeCount() {
            return likeCount;
        }
    }
}

