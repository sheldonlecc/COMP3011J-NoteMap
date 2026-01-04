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
import com.noworld.notemap.data.dto.NotificationResponse;
import com.noworld.notemap.data.dto.ChatMessageResponse;
import com.noworld.notemap.data.dto.ConversationResponse;
import com.noworld.notemap.data.dto.SendMessageRequest;

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

    // Existing endpoints

    // Comments: list for a note
    @GET("/api/notes/{id}/comments")
    Call<List<CommentResponse>> getComments(@Path("id") String noteId);

    // Comments: add
    @POST("/api/notes/{id}/comments")
    Call<CommentResponse> addComment(@Path("id") String noteId, @Body AddCommentRequest request);

    // Comments: like/unlike
    @POST("/api/comments/{id}/like")
    Call<LikeResponse> toggleCommentLike(@Path("id") String commentId);

    // Comments: delete
    @DELETE("/api/comments/{id}")
    Call<Void> deleteComment(@Path("id") String commentId);

    // Notes: update content or visibility
    @PUT("/api/notes/{id}")
    Call<Void> updateNote(@Path("id") String id, @Body com.noworld.notemap.data.dto.UpdateNoteRequest request);

    // Notes: delete
    @DELETE("/api/notes/{id}")
    Call<Void> deleteNote(@Path("id") String id);

    // Notifications
    @GET("/api/notifications")
    Call<List<NotificationResponse>> getNotifications(@Query("page") Integer page, @Query("size") Integer size);

    @GET("/api/notifications/unread_count")
    Call<java.util.Map<String, Integer>> getNotificationUnreadCount();

    @POST("/api/notifications/read_all")
    Call<Void> readAllNotifications();

    // Direct chat
    @GET("/api/chats")
    Call<List<ConversationResponse>> getConversations();

    @GET("/api/chats/{peerId}/messages")
    Call<List<ChatMessageResponse>> getMessages(@Path("peerId") String peerId,
                                                @Query("sinceId") String sinceId);

    @POST("/api/chats/{peerId}/messages")
    Call<ChatMessageResponse> sendMessage(@Path("peerId") String peerId,
                                          @Body SendMessageRequest request);
}
