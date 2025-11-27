package com.noworld.notemap.data;

import com.noworld.notemap.data.dto.LikeResponse;
import com.noworld.notemap.data.dto.LoginRequest;
import com.noworld.notemap.data.dto.LoginResponse;
import com.noworld.notemap.data.dto.RegisterRequest;
import com.noworld.notemap.data.dto.MapNoteResponse;
import com.noworld.notemap.data.dto.OssPresignRequest;
import com.noworld.notemap.data.dto.OssPresignResponse;
import com.noworld.notemap.data.dto.PublishNoteRequest;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

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
}
