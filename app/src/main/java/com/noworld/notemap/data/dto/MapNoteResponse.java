package com.noworld.notemap.data.dto;

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
    public String authorAvatarUrl;
    public int likeCount;
}
