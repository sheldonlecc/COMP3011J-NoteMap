package com.amap.apis.cluster.demo;

import com.amap.api.maps.model.LatLng;
import com.amap.apis.cluster.ClusterItem;
import java.io.Serializable;
import java.util.List;

public class RegionItem implements ClusterItem, Serializable {

    private static final long serialVersionUID = 20251109L;

    // 1. 地图数据
    private transient LatLng mLatLng;
    private double mLatitude;
    private double mLongitude;

    // 2. 笔记数据
    private String noteId;
    private String title;
    private String photoUrl;
    private String authorName;
    private String authorAvatarUrl;
    private int likeCount;
    private boolean likedByCurrentUser;

    // 3. 详情页所需数据
    private String description;     // 笔记正文
    private String noteType;        // 笔记类型
    private String locationName;    // 地点名称
    private List<String> imageUrls; // 多图列表

    // 4. 【关键新增】发布时间字段 (解决报错的核心)
    private String createTime;

    private boolean isPrivate;

    private String authorId;



    // 构造函数
    public RegionItem(LatLng latLng, String noteId, String title, String photoUrl,
                      String authorName, String authorAvatarUrl, int likeCount,
                      String description, String noteType, String locationName) {
        this.mLatLng = latLng;
        this.mLatitude = latLng.latitude;
        this.mLongitude = latLng.longitude;
        this.noteId = noteId;
        this.title = title;
        this.photoUrl = photoUrl;
        this.authorName = authorName;
        this.authorAvatarUrl = authorAvatarUrl;
        this.likeCount = likeCount;
        this.description = description;
        this.noteType = noteType;
        this.locationName = locationName;
        this.likedByCurrentUser = false;
        // 默认为空，由 MainActivity setCreateTime 注入
        this.createTime = "2023-11-27";
    }

    // 兼容旧构造函数
    public RegionItem(LatLng latLng, String title) {
        this(latLng, "id_" + title, title, null,
                "Test user", null, (int)(Math.random() * 100),
                "Sample note content...",
                "Scenery",
                "Chaoyang, Beijing"
        );
    }

    // --- Getters & Setters ---

    @Override
    public LatLng getPosition() {
        if (mLatLng == null) {
            mLatLng = new LatLng(mLatitude, mLongitude);
        }
        return mLatLng;
    }

    // 【关键新增】Getter 和 Setter 方法
    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    // 其他 Getters
    public String getNoteId() { return noteId; }
    public String getTitle() { return title; }
    public String getPhotoUrl() { return photoUrl; }
    public String getAuthorName() { return authorName; }
    public String getAuthorAvatarUrl() { return authorAvatarUrl; }
    public int getLikeCount() { return likeCount; }
    public void setLikeCount(int likeCount) { this.likeCount = Math.max(0, likeCount); }
    public boolean isLikedByCurrentUser() { return likedByCurrentUser; }
    public void setLikedByCurrentUser(boolean likedByCurrentUser) { this.likedByCurrentUser = likedByCurrentUser; }
    public String getDescription() { return description; }
    public String getNoteType() { return noteType; }
    public String getLocationName() { return locationName; }
    public List<String> getImageUrls() { return imageUrls; }
    public void setImageUrls(List<String> imageUrls) { this.imageUrls = imageUrls; }

    // 在 RegionItem.java 中添加
    public void setAuthorAvatarUrl(String authorAvatarUrl) {
        this.authorAvatarUrl = authorAvatarUrl;
    }
    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    // 【新增】Getter 和 Setter
    public boolean isPrivate() {
        return isPrivate;
    }

    public void setPrivate(boolean isPrivate) {
        this.isPrivate = isPrivate;
    }

    public String getAuthorId() {
        return authorId;
    }

    // 3. 添加 setAuthorId() 方法 (用于解析 JSON 时设置值)
    public void setAuthorId(String authorId) {
        this.authorId = authorId;
    }

    public double getLatitude() {
        return getPosition().latitude;
    }

    public double getLongitude() {
        return getPosition().longitude;
    }


}
