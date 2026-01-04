package com.noworld.notemap.data.dto;

import com.google.gson.annotations.SerializedName; // Important: use this annotation
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

    public String createTime;

    @SerializedName(value = "authorAvatarUrl", alternate = {"avatar", "avatarUrl", "userAvatar", "author_avatar"})
    public String authorAvatarUrl;

    public int likeCount;

    // Added: private flag mapped from backend is_private
    public boolean isPrivate;
}
