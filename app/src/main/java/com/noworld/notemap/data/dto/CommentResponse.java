package com.noworld.notemap.data.dto;

import com.google.gson.annotations.SerializedName;

/**
 * 后端返回的评论结构。
 */
public class CommentResponse {
    public String id;
    public String content;

    @SerializedName(value = "userName", alternate = {"username", "authorName", "nickName"})
    public String userName;

    @SerializedName(value = "avatarUrl", alternate = {"userAvatar", "authorAvatar"})
    public String avatarUrl;

    @SerializedName(value = "createdAt", alternate = {"createTime", "time"})
    public String createdAt;
}
