package com.noworld.notemap.data;

import com.noworld.notemap.data.dto.LikeResponse;
import com.noworld.notemap.data.dto.LoginRequest;
import com.noworld.notemap.data.dto.LoginResponse;
import com.noworld.notemap.data.dto.RegisterRequest;
import com.noworld.notemap.data.dto.MapNoteResponse;
import com.noworld.notemap.data.dto.OssPresignRequest;
import com.noworld.notemap.data.dto.OssPresignResponse;
import com.noworld.notemap.data.dto.PublishNoteRequest;
import com.noworld.notemap.data.dto.UpdateProfileRequest;
import com.noworld.notemap.data.dto.UpdateProfileResponse;
import com.noworld.notemap.data.dto.AddCommentRequest;
import com.noworld.notemap.data.dto.CommentResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.PUT;
import retrofit2.http.DELETE;

public interface  ApiService {

    @POST("/api/auth/login")
    Call<LoginResponse> login(@Body LoginRequest request);

    @POST("/api/auth/register")
    Call<LoginResponse> register(@Body RegisterRequest request);

    @GET("/api/notes")
    Call<List<MapNoteResponse>> getNotes(@Query("lat") Double lat,
                                         @Query("lng") Double lng,
                                         @Query("keyword") String keyword,
                                         @Query("type") String type);

    @POST("/api/notes")
    Call<MapNoteResponse> publishNote(@Body PublishNoteRequest request);

    @POST("/api/notes/{id}/like")
    Call<LikeResponse> toggleLike(@Path("id") String noteId);

    @POST("/api/oss/presign")
    Call<OssPresignResponse> getOssPresign(@Body OssPresignRequest request);

    @POST("/api/auth/update")
    Call<UpdateProfileResponse> updateProfile(@Body UpdateProfileRequest request);

    // ... 原有的代码 ...

    // 【评论】获取笔记评论列表
    @GET("/api/notes/{id}/comments")
    Call<List<CommentResponse>> getComments(@Path("id") String noteId);

    // 【评论】发表评论
    @POST("/api/notes/{id}/comments")
    Call<CommentResponse> addComment(@Path("id") String noteId, @Body AddCommentRequest request);

    // 【评论】点赞/取消点赞
    @POST("/api/comments/{id}/like")
    Call<LikeResponse> toggleCommentLike(@Path("id") String commentId);

    // 【新增】修改笔记 (内容 或 可见性)
    @PUT("/api/notes/{id}")
    Call<Void> updateNote(@Path("id") String id, @Body com.noworld.notemap.data.dto.UpdateNoteRequest request);

    // 【新增】删除笔记
    @DELETE("/api/notes/{id}")
    Call<Void> deleteNote(@Path("id") String id);
}
