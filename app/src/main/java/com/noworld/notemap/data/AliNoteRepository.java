package com.noworld.notemap.data;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import com.amap.apis.cluster.demo.RegionItem;
import com.noworld.notemap.data.dto.LikeResponse;
import com.noworld.notemap.data.dto.MapNoteResponse;
import com.noworld.notemap.data.dto.OssPresignRequest;
import com.noworld.notemap.data.dto.OssPresignResponse;
import com.noworld.notemap.data.dto.PublishNoteRequest;
import com.noworld.notemap.data.dto.AddCommentRequest;
import com.noworld.notemap.data.dto.CommentResponse;
import com.noworld.notemap.data.model.CommentItem;

import java.io.InputStream;
import java.net.UnknownServiceException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Data repository for Aliyun-backed APIs.
 */
public class AliNoteRepository {

    public interface NotesCallback {
        void onSuccess(List<RegionItem> items);

        void onError(@NonNull Throwable throwable);
    }

    public interface UploadCallback {
        void onSuccess(String fileUrl);

        void onError(@NonNull Throwable throwable);
    }

    public interface PublishCallback {
        void onSuccess();

        void onError(@NonNull Throwable throwable);
    }

    public interface LikeCallback {
        void onResult(boolean liked, int likeCount);

        void onRequireLogin();

        void onError(@NonNull Throwable throwable);
    }

    public interface CommentsCallback {
        void onSuccess(List<CommentItem> comments);

        void onError(@NonNull Throwable throwable);
    }

    public interface AddCommentCallback {
        void onSuccess(CommentItem newComment);

        void onRequireLogin();

        void onError(@NonNull Throwable throwable);
    }

    public interface CommentLikeCallback {
        void onResult(boolean liked, int likeCount);

        void onRequireLogin();

        void onError(@NonNull Throwable throwable);
    }

    public interface CommentDeleteCallback {
        void onSuccess();

        void onRequireLogin();

        void onError(@NonNull Throwable throwable);
    }

    private static final String TAG = "AliNoteRepository";
    private static AliNoteRepository INSTANCE;
    private final ApiService api;
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();
    private final OkHttpClient uploadClient = new OkHttpClient.Builder().build();
    private final TokenStore tokenStore;
    private final Context appContext;
    private final UserStore userStore;
    private final LikedStore likedStore;

    private AliNoteRepository(Context context) {
        this.appContext = context.getApplicationContext();
        this.api = ApiClient.getService(appContext);
        this.tokenStore = TokenStore.getInstance(appContext);
        this.userStore = UserStore.getInstance(appContext);
        this.likedStore = LikedStore.getInstance(appContext);
    }

    public static synchronized AliNoteRepository getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = new AliNoteRepository(context);
        }
        return INSTANCE;
    }

    public void fetchNotes(Double lat, Double lng, String keyword, String type, NotesCallback callback) {
        api.getNotes(lat, lng, keyword, type).enqueue(new Callback<List<MapNoteResponse>>() {
            @Override
            public void onResponse(@NonNull Call<List<MapNoteResponse>> call, @NonNull Response<List<MapNoteResponse>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onError(new IllegalStateException("Failed to fetch notes"));
                    return;
                }
                List<RegionItem> items = new ArrayList<>();
                String uid = getCurrentUid();
                Set<String> likedIds = likedStore.getLikedIds(uid);

                // Ensure RegionItem carries author id and privacy state
                for (MapNoteResponse r : response.body()) {
                    MapNote note = MapNote.fromResponse(r);
                    RegionItem regionItem = note.toRegionItem();

                    // MapNote.fromResponse maps authorId/isPrivate; RegionItem receives them here.

                    if (likedIds.contains(regionItem.getNoteId())) {
                        regionItem.setLikedByCurrentUser(true);
                    }
                    items.add(regionItem);
                }
                callback.onSuccess(items);
            }

            @Override
            public void onFailure(@NonNull Call<List<MapNoteResponse>> call, @NonNull Throwable t) {
                callback.onError(t);
            }
        });
    }

    public interface MapNotesCallback {
        void onSuccess(List<MapNote> notes);

        void onError(@NonNull Throwable throwable);
    }

    /**
     * Fetch all notes and return MapNote list (used for filtering in "Mine").
     */
    public void fetchAllNotes(MapNotesCallback callback) {
        api.getNotes(null, null, null, null).enqueue(new Callback<List<MapNoteResponse>>() {
            @Override
            public void onResponse(@NonNull Call<List<MapNoteResponse>> call, @NonNull Response<List<MapNoteResponse>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onError(new IllegalStateException("Failed to fetch notes"));
                    return;
                }
                List<MapNote> list = new ArrayList<>();
                String uid = getCurrentUid();
                Set<String> likedIds = likedStore.getLikedIds(uid);

                // Use backend data directly
                for (MapNoteResponse r : response.body()) {
                    MapNote note = MapNote.fromResponse(r);
                    RegionItem regionItem = note.toRegionItem();

                    // Keep authorId/privacy info aligned with MapNote.fromResponse

                    if (likedIds.contains(regionItem.getNoteId())) {
                        regionItem.setLikedByCurrentUser(true);
                    }
                    list.add(note);
                }
                callback.onSuccess(list);
            }

            @Override
            public void onFailure(@NonNull Call<List<MapNoteResponse>> call, @NonNull Throwable t) {
                callback.onError(t);
            }
        });
    }

    public void publishNote(MapNote note, PublishCallback callback) {
        PublishNoteRequest request = new PublishNoteRequest(
                note.getTitle(),
                note.getDescription(),
                note.getType(),
                note.getLatitude(),
                note.getLongitude(),
                note.getLocationName(),
                note.getImageUrls()
        );
        api.publishNote(request).enqueue(new Callback<MapNoteResponse>() {
            @Override
            public void onResponse(@NonNull Call<MapNoteResponse> call, @NonNull Response<MapNoteResponse> response) {
                if (response.isSuccessful()) {
                    callback.onSuccess();
                } else {
                    callback.onError(new IllegalStateException("Publish failed"));
                }
            }

            @Override
            public void onFailure(@NonNull Call<MapNoteResponse> call, @NonNull Throwable t) {
                callback.onError(t);
            }
        });
    }

    public void uploadImage(Uri imageUri, UploadCallback callback) {
        ioExecutor.execute(() -> {
            try {
                String fileName = "note_" + System.currentTimeMillis() + ".jpg";
                long size = getFileSize(imageUri);
                String mime = appContext.getContentResolver().getType(imageUri);
                OssPresignRequest request = new OssPresignRequest(fileName, size, mime != null ? mime : "image/jpeg");

                // Debug log 1: request presigned URL
                Log.d(TAG, "Step 1: Requesting presigned URL for " + fileName);

                Response<OssPresignResponse> presignResp = api.getOssPresign(request).execute();

                // Debug log 2: presign response
                Log.d(TAG, "Step 2: Presign response received. Success=" + presignResp.isSuccessful());

                if (!presignResp.isSuccessful() || presignResp.body() == null) {
                    callback.onError(new IllegalStateException("Failed to get upload credentials"));
                    return;
                }
                OssPresignResponse presign = presignResp.body();

                // Debug log 3: starting upload
                Log.d(TAG, "Step 3: Starting actual upload to URL: " + presign.uploadUrl);

                boolean uploadOk = uploadToOss(presign, imageUri);
                if (!uploadOk) {
                    callback.onError(new IllegalStateException("Upload failed"));
                    return;
                }
                callback.onSuccess(presign.fileUrl);
            } catch (Exception e) {
                callback.onError(e);
            }
        });
    }

    private boolean uploadToOss(OssPresignResponse presign, Uri imageUri) {
        if (presign == null || presign.uploadUrl == null) return false;
        String uploadUrl = presign.uploadUrl;
        boolean triedHttps = false;
        while (true) {
            try {
                byte[] data = readAllBytes(imageUri);
                RequestBody body = RequestBody.create(data, MediaType.parse("image/jpeg"));
                Headers headers = presign.headers != null ? Headers.of(presign.headers) : new Headers.Builder().build();
                Log.d(TAG, "uploadToOss url=" + uploadUrl + " size=" + data.length + " headers=" + headers);
                Request request = new Request.Builder()
                        .url(uploadUrl)
                        .headers(headers)
                        .put(body)
                        .build();
                try (okhttp3.Response resp = uploadClient.newCall(request).execute()) {
                    if (!resp.isSuccessful()) {
                        String errBody = resp.body() != null ? resp.body().string() : "";
                        Log.e(TAG, "uploadToOss failed code=" + resp.code() + " msg=" + resp.message() + " body=" + errBody);
                        return false;
                    }
                    return true;
                }
            } catch (UnknownServiceException e) {
                // If cleartext is rejected, retry with https
                if (!triedHttps && uploadUrl.startsWith("http://")) {
                    uploadUrl = uploadUrl.replaceFirst("^http://", "https://");
                    triedHttps = true;
                    continue;
                }
                Log.e(TAG, "uploadToOss error", e);
                return false;
            } catch (Exception e) {
                Log.e(TAG, "uploadToOss error", e);
                return false;
            }
        }
    }

    private long getFileSize(Uri uri) throws Exception {
        try (InputStream in = appContext.getContentResolver().openInputStream(uri)) {
            if (in == null) return 0;
            long total = 0;
            byte[] buf = new byte[4096];
            int len;
            while ((len = in.read(buf)) != -1) {
                total += len;
            }
            return total;
        }
    }

    private byte[] readAllBytes(Uri uri) throws Exception {
        try (InputStream in = appContext.getContentResolver().openInputStream(uri)) {
            if (in == null) return new byte[0];
            byte[] buf = new byte[in.available() > 0 ? in.available() : 8192];
            int len;
            int offset = 0;
            while ((len = in.read(buf, offset, buf.length - offset)) != -1) {
                offset += len;
                if (offset == buf.length) {
                    byte[] newBuf = new byte[buf.length * 2];
                    System.arraycopy(buf, 0, newBuf, 0, buf.length);
                    buf = newBuf;
                }
            }
            byte[] result = new byte[offset];
            System.arraycopy(buf, 0, result, 0, offset);
            return result;
        }
    }

    public void toggleLike(RegionItem item, LikeCallback callback) {
        String token = tokenStore.getToken();
        if (token == null || token.isEmpty()) {
            callback.onRequireLogin();
            return;
        }
        api.toggleLike(item.getNoteId()).enqueue(new Callback<LikeResponse>() {
            @Override
            public void onResponse(@NonNull Call<LikeResponse> call, @NonNull Response<LikeResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    LikeResponse body = response.body();
                    String uid = getCurrentUid();
                    String noteId = item.getNoteId();
                    if (noteId != null && !noteId.isEmpty()) {
                        likedStore.toggle(uid, noteId, body.liked);
                    }
                    callback.onResult(body.liked, body.likeCount);
                } else {
                    callback.onError(new IllegalStateException("Like operation failed"));
                }
            }

            @Override
            public void onFailure(@NonNull Call<LikeResponse> call, @NonNull Throwable t) {
                callback.onError(t);
            }
        });
    }

    // Simple generic callback
    public interface SimpleCallback {
        void onSuccess();
        void onError(Throwable t);
    }

    // Update user info through backend
    public void updateUserInfo(String nickname, String avatarUrl, SimpleCallback callback) {
        // Build request body (DTO)
        com.noworld.notemap.data.dto.UpdateProfileRequest request =
                new com.noworld.notemap.data.dto.UpdateProfileRequest(nickname, avatarUrl);

        // Make request
        api.updateProfile(request).enqueue(new retrofit2.Callback<com.noworld.notemap.data.dto.UpdateProfileResponse>() {
            @Override
            public void onResponse(@NonNull Call<com.noworld.notemap.data.dto.UpdateProfileResponse> call,
                                   @NonNull Response<com.noworld.notemap.data.dto.UpdateProfileResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess();
                } else {
                    callback.onError(new IllegalStateException("Update failed: " + response.code()));
                }
            }

            @Override
            public void onFailure(@NonNull Call<com.noworld.notemap.data.dto.UpdateProfileResponse> call, @NonNull Throwable t) {
                callback.onError(t);
            }
        });
    }

    public void fetchComments(String noteId, CommentsCallback callback) {
        api.getComments(noteId).enqueue(new Callback<List<CommentResponse>>() {
            @Override
            public void onResponse(@NonNull Call<List<CommentResponse>> call, @NonNull Response<List<CommentResponse>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onError(new IllegalStateException("Failed to fetch comments"));
                    return;
                }
                List<CommentItem> result = new ArrayList<>();
                for (CommentResponse resp : response.body()) {
                    result.add(mapToCommentItem(resp));
                }
                callback.onSuccess(result);
            }

            @Override
            public void onFailure(@NonNull Call<List<CommentResponse>> call, @NonNull Throwable t) {
                callback.onError(t);
            }
        });
    }

    public void addComment(String noteId, String content, String parentId, AddCommentCallback callback) {
        String token = tokenStore.getToken();
        if (token == null || token.isEmpty()) {
            callback.onRequireLogin();
            return;
        }
        api.addComment(noteId, new AddCommentRequest(content, parentId)).enqueue(new Callback<CommentResponse>() {
            @Override
            public void onResponse(@NonNull Call<CommentResponse> call, @NonNull Response<CommentResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(mapToCommentItem(response.body()));
                } else if (response.code() == 401) {
                    callback.onRequireLogin();
                } else {
                    callback.onError(new IllegalStateException("Comment failed"));
                }
            }

            @Override
            public void onFailure(@NonNull Call<CommentResponse> call, @NonNull Throwable t) {
                callback.onError(t);
            }
        });
    }

    public void addComment(String noteId, String content, AddCommentCallback callback) {
        addComment(noteId, content, null, callback);
    }

    public void toggleCommentLike(String commentId, CommentLikeCallback callback) {
        String token = tokenStore.getToken();
        if (token == null || token.isEmpty()) {
            callback.onRequireLogin();
            return;
        }
        api.toggleCommentLike(commentId).enqueue(new Callback<LikeResponse>() {
            @Override
            public void onResponse(@NonNull Call<LikeResponse> call, @NonNull Response<LikeResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onResult(response.body().liked, response.body().likeCount);
                } else {
                    callback.onError(new IllegalStateException("Operation failed"));
                }
            }

            @Override
            public void onFailure(@NonNull Call<LikeResponse> call, @NonNull Throwable t) {
                callback.onError(t);
            }
        });
    }

    public void deleteComment(String commentId, CommentDeleteCallback callback) {
        String token = tokenStore.getToken();
        if (token == null || token.isEmpty()) {
            callback.onRequireLogin();
            return;
        }
        api.deleteComment(commentId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (response.isSuccessful()) {
                    callback.onSuccess();
                } else if (response.code() == 401) {
                    callback.onRequireLogin();
                } else {
                    callback.onError(new IllegalStateException("Delete failed"));
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                callback.onError(t);
            }
        });
    }

    private CommentItem mapToCommentItem(CommentResponse resp) {
        if (resp == null) return new CommentItem("", "Unknown user", "", "", null, null, null, null, 0, false);
        return new CommentItem(
                resp.id != null ? resp.id : "",
                resp.userName != null ? resp.userName : "Map user",
                resp.content != null ? resp.content : "",
                resp.createdAt != null ? resp.createdAt : "",
                resp.avatarUrl,
                resp.parentId,
                resp.replyToUserName,
                resp.authorId,
                resp.likeCount != null ? resp.likeCount : 0,
                resp.liked != null && resp.liked
        );
    }

    private String getCurrentUid() {
        String token = tokenStore.getToken();
        String tokenUid = userStore.extractUidFromToken(token);
        if (tokenUid != null && !tokenUid.isEmpty()) {
            userStore.ensureUid(tokenUid);
            return tokenUid;
        }
        String uid = userStore.getUid();
        if (uid != null && !uid.isEmpty()) {
            return uid;
        }
        return "guest";
    }

    // Delete a note
    public void deleteNote(String noteId, SimpleCallback callback) {
        api.deleteNote(noteId).enqueue(new retrofit2.Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (response.isSuccessful()) {
                    callback.onSuccess();
                } else {
                    callback.onError(new Exception("Delete failed: " + response.code()));
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                callback.onError(t);
            }
        });
    }

    // Update note visibility (public/private)
    public void setNotePrivacy(String noteId, boolean isPrivate, SimpleCallback callback) {
        com.noworld.notemap.data.dto.UpdateNoteRequest request =
                new com.noworld.notemap.data.dto.UpdateNoteRequest(isPrivate);

        api.updateNote(noteId, request).enqueue(new retrofit2.Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (response.isSuccessful()) {
                    callback.onSuccess();
                } else {
                    callback.onError(new Exception("Setting failed: " + response.code()));
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                callback.onError(t);
            }
        });
    }

    // Update note content (title/description)
    public void updateNote(String noteId, com.noworld.notemap.data.dto.UpdateNoteRequest request, SimpleCallback callback) {
        // Reuse updateNote endpoint; backend should patch only provided fields
        api.updateNote(noteId, request).enqueue(new retrofit2.Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (response.isSuccessful()) {
                    callback.onSuccess();
                } else {
                    callback.onError(new Exception("Update failed: " + response.code()));
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                callback.onError(t);
            }
        });
    }
}
