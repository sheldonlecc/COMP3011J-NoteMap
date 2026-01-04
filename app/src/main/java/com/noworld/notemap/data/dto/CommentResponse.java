package com.noworld.notemap.data.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Comment structure returned by the backend.
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

    @SerializedName(value = "parentId", alternate = {"parent_id"})
    public String parentId;

    @SerializedName(value = "replyToUserName", alternate = {"replyTo", "reply_to_user_name", "replyUserName"})
    public String replyToUserName;

    @SerializedName(value = "likeCount", alternate = {"like_count"})
    public Integer likeCount;

    @SerializedName(value = "liked", alternate = {"isLiked"})
    public Boolean liked;

    @SerializedName(value = "authorId", alternate = {"userId", "uid", "user_id"})
    public String authorId;
}
