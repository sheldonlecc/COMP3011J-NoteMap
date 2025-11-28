package com.noworld.notemap.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amap.api.maps.model.LatLng;
import com.amap.apis.cluster.demo.RegionItem;
import com.noworld.notemap.data.dto.MapNoteResponse;

import java.util.ArrayList;
import java.util.List;

/**
 * Firestore 中 notes 文档的数据模型。
 */
public class MapNote {

    public static final String FIELD_TITLE = "title";
    public static final String FIELD_DESCRIPTION = "description";
    public static final String FIELD_TYPE = "type";
    public static final String FIELD_GEO_POINT = "geo_point";
    public static final String FIELD_LOCATION_NAME = "location_name";
    public static final String FIELD_IMAGE_URLS = "image_urls";
    public static final String FIELD_AUTHOR_ID = "author_id";
    public static final String FIELD_AUTHOR_NAME = "author_name";
    public static final String FIELD_AUTHOR_AVATAR = "author_avatar";
    public static final String FIELD_TIMESTAMP = "timestamp";
    public static final String FIELD_LIKE_COUNT = "like_count";

    private String id;
    private String title;
    private String description;
    private String type;
    private double latitude;
    private double longitude;
    private String locationName;
    private List<String> imageUrls;
    private String authorId;
    private String authorName;
    private String authorAvatarUrl;
    private int likeCount;
    private long timestamp;

    private boolean isPrivate;

    private String createTime; // 【新增】

    public MapNote() {
    }

    public MapNote(String title, String description, String type, double latitude, double longitude,
                   String locationName, List<String> imageUrls, String authorId,
                   String authorName, String authorAvatarUrl) {
        this.title = title;
        this.description = description;
        this.type = type;
        this.latitude = latitude;
        this.longitude = longitude;
        this.locationName = locationName;
        this.imageUrls = imageUrls;
        this.authorId = authorId;
        this.authorName = authorName;
        this.authorAvatarUrl = authorAvatarUrl;
        this.likeCount = 0;
    }

    public static MapNote fromResponse(@NonNull MapNoteResponse response) {
        MapNote note = new MapNote();
        note.id = response.id;
        note.title = response.title;
        note.description = response.description;
        note.type = response.type;
        note.latitude = response.latitude;
        note.longitude = response.longitude;
        note.locationName = response.locationName;
        note.imageUrls = response.imageUrls != null ? response.imageUrls : new ArrayList<>();
        note.authorId = response.authorId;
        note.authorName = response.authorName;
        note.authorAvatarUrl = response.authorAvatarUrl;
        note.likeCount = response.likeCount;

        // 【fromResponse 赋值】
        note.isPrivate = response.isPrivate;

        note.createTime = response.createTime; // 【新增】赋值
        return note;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Nullable
    public String getCoverUrl() {
        if (imageUrls == null || imageUrls.isEmpty()) {
            return null;
        }
        return imageUrls.get(0);
    }

    public RegionItem toRegionItem() {
        LatLng latLng = new LatLng(latitude, longitude);
        RegionItem item = new RegionItem(
                latLng,
                id != null ? id : "temp_" + System.nanoTime(),
                title,
                getCoverUrl(),
                authorName != null ? authorName : "地图用户",
                authorAvatarUrl,
                likeCount,
                description,
                type,
                locationName
        );
        item.setImageUrls(imageUrls);

        // 【toRegionItem 赋值】
        // 关键：将状态传递给 RegionItem
        item.setAuthorId(authorId);
        item.setPrivate(isPrivate); // 调用 RegionItem 的 setPrivate 方法

        item.setCreateTime(createTime); // 【新增】传递给 RegionItem

        return item;
    }

    public String getId() {
        return id;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public String getLocationName() {
        return locationName;
    }

    public List<String> getImageUrls() {
        return imageUrls;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getType() {
        return type;
    }

    public String getAuthorName() {
        return authorName;
    }

    public String getAuthorAvatarUrl() {
        return authorAvatarUrl;
    }

    public String getAuthorId() {
        return authorId;
    }

    public int getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(int likeCount) {
        this.likeCount = Math.max(0, likeCount);
    }

    // 【新增 Getter】
    public boolean isPrivate() {
        return isPrivate;
    }

    // 【新增 Setter】
    public void setPrivate(boolean isPrivate) {
        this.isPrivate = isPrivate;
    }

}
