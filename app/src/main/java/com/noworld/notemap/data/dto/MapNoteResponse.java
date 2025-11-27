package com.noworld.notemap.data.dto;

import com.google.gson.annotations.SerializedName; // 务必导入这个包
import java.util.List;

public class MapNoteResponse {
    public String id;
    public String title;
    public String description;
    public String type;
    public double latitude;
    public double longitude;
    public String locationName;
    public List<String> imageUrls;
    public String authorId;
    public String authorName;

    // 【关键修改】
    // 请确认后端 JSON 里，头像那个字段到底叫什么？
    // 情况 A：后端叫 "avatar" (最常见) -> 写 @SerializedName("avatar")
    // 情况 B：后端叫 "authorAvatarUrl" (完全一致) -> 写 @SerializedName("authorAvatarUrl")
    // 情况 C：后端叫 "user_avatar" -> 写 @SerializedName("user_avatar")

    // 我先帮您写成 "avatar"，因为这是很多后端的习惯。
    // 如果您的后端返回的是 "authorAvatarUrl"，请手动改一下括号里的字。
    @SerializedName(value = "authorAvatarUrl", alternate = {"avatar", "avatarUrl", "userAvatar", "author_avatar"})
    public String authorAvatarUrl;

    public int likeCount;
}