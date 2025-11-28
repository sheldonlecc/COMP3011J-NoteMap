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
 * 面向阿里云接口的数据仓库。
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
                    callback.onError(new IllegalStateException("拉取笔记失败"));
                    return;
                }
                List<RegionItem> items = new ArrayList<>();
                String uid = getCurrentUid();
                Set<String> likedIds = likedStore.getLikedIds(uid);

                // 【已修改】确保 RegionItem 获得作者ID和私密状态
                for (MapNoteResponse r : response.body()) {
                    MapNote note = MapNote.fromResponse(r);
                    RegionItem regionItem = note.toRegionItem();

                    // 确保 RegionItem 得到作者ID和私密状态
                    // 假设 MapNoteResponse 包含 authorId 和 isPrivate 字段
                    // 并且 MapNote.fromResponse 已经将它们赋给了 note
                    // 这里只需要确保 RegionItem 接收到它们

                    // *如果 RegionItem 的 setAuthorId 和 setPrivate 缺失*
                    // *这将在 MapNote.toRegionItem() 中解决，但为确保兼容性，先依赖 MapNote*

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
        * 拉取所有笔记并返回 MapNote 列表，方便在“我的”页过滤。
        */
    public void fetchAllNotes(MapNotesCallback callback) {
        api.getNotes(null, null, null, null).enqueue(new Callback<List<MapNoteResponse>>() {
            @Override
            public void onResponse(@NonNull Call<List<MapNoteResponse>> call, @NonNull Response<List<MapNoteResponse>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onError(new IllegalStateException("拉取笔记失败"));
                    return;
                }
                List<MapNote> list = new ArrayList<>();
                String uid = getCurrentUid();
                Set<String> likedIds = likedStore.getLikedIds(uid);

                // 【已修改】直接使用后端返回的数据
                for (MapNoteResponse r : response.body()) {
                    MapNote note = MapNote.fromResponse(r);
                    RegionItem regionItem = note.toRegionItem();

                    // 确保 RegionItem 获得作者ID和私密状态
                    // 与 fetchNotes 类似，确保 MapNote.fromResponse(r) 成功映射了 r.authorId

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
                    callback.onError(new IllegalStateException("发布失败"));
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

                // <<< 在这里添加调试日志 1 >>>
                Log.d(TAG, "Step 1: Requesting presigned URL for " + fileName);

                Response<OssPresignResponse> presignResp = api.getOssPresign(request).execute();

                // <<< 在这里添加调试日志 2 >>>
                Log.d(TAG, "Step 2: Presign response received. Success=" + presignResp.isSuccessful());

                if (!presignResp.isSuccessful() || presignResp.body() == null) {
                    callback.onError(new IllegalStateException("获取上传凭证失败"));
                    return;
                }
                OssPresignResponse presign = presignResp.body();

                // <<< 在这里添加调试日志 3 >>>
                Log.d(TAG, "Step 3: Starting actual upload to URL: " + presign.uploadUrl);

                boolean uploadOk = uploadToOss(presign, imageUri);
                if (!uploadOk) {
                    callback.onError(new IllegalStateException("上传失败"));
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
                // Cleartext 被拒绝时尝试换用 https
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
                    callback.onError(new IllegalStateException("点赞失败"));
                }
            }

            @Override
            public void onFailure(@NonNull Call<LikeResponse> call, @NonNull Throwable t) {
                callback.onError(t);
            }
        });
    }

    // === 新增：定义一个简单的回调接口 ===
    public interface SimpleCallback {
        void onSuccess();
        void onError(Throwable t);
    }

    // === 新增：调用后端更新用户信息的接口 ===
    public void updateUserInfo(String nickname, String avatarUrl, SimpleCallback callback) {
        // 构建请求体 (DTO)
        com.noworld.notemap.data.dto.UpdateProfileRequest request =
                new com.noworld.notemap.data.dto.UpdateProfileRequest(nickname, avatarUrl);

        // 发起请求
        api.updateProfile(request).enqueue(new retrofit2.Callback<com.noworld.notemap.data.dto.UpdateProfileResponse>() {
            @Override
            public void onResponse(@NonNull Call<com.noworld.notemap.data.dto.UpdateProfileResponse> call,
                                   @NonNull Response<com.noworld.notemap.data.dto.UpdateProfileResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess();
                } else {
                    callback.onError(new IllegalStateException("更新失败: " + response.code()));
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
                    callback.onError(new IllegalStateException("获取评论失败"));
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
                    callback.onError(new IllegalStateException("评论失败"));
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

    private CommentItem mapToCommentItem(CommentResponse resp) {
        if (resp == null) return new CommentItem("", "未知用户", "", "", null, null, null);
        return new CommentItem(
                resp.id != null ? resp.id : "",
                resp.userName != null ? resp.userName : "地图用户",
                resp.content != null ? resp.content : "",
                resp.createdAt != null ? resp.createdAt : "",
                resp.avatarUrl,
                resp.parentId,
                resp.replyToUserName
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

    // === 新增：删除笔记 ===
    public void deleteNote(String noteId, SimpleCallback callback) {
        api.deleteNote(noteId).enqueue(new retrofit2.Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (response.isSuccessful()) {
                    callback.onSuccess();
                } else {
                    callback.onError(new Exception("删除失败: " + response.code()));
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                callback.onError(t);
            }
        });
    }

    // === 新增：修改笔记可见性 (公开/私密) ===
    public void setNotePrivacy(String noteId, boolean isPrivate, SimpleCallback callback) {
        com.noworld.notemap.data.dto.UpdateNoteRequest request =
                new com.noworld.notemap.data.dto.UpdateNoteRequest(isPrivate);

        api.updateNote(noteId, request).enqueue(new retrofit2.Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (response.isSuccessful()) {
                    callback.onSuccess();
                } else {
                    callback.onError(new Exception("设置失败: " + response.code()));
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                callback.onError(t);
            }
        });
    }

    // === 新增：修改笔记内容 (标题和描述) ===
    public void updateNote(String noteId, com.noworld.notemap.data.dto.UpdateNoteRequest request, SimpleCallback callback) {
        // 注意：这里我们重用了 updateNote 接口，后端应能识别请求体内容并只更新相应字段
        api.updateNote(noteId, request).enqueue(new retrofit2.Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (response.isSuccessful()) {
                    callback.onSuccess();
                } else {
                    callback.onError(new Exception("更新失败: " + response.code()));
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                callback.onError(t);
            }
        });
    }
}
