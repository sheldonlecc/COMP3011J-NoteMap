// 位置: com/amap/apis/cluster/demo/RegionItem.java

package com.amap.apis.cluster.demo;

import com.amap.api.maps.model.LatLng;
import com.amap.apis.cluster.ClusterItem;
import java.io.Serializable;

public class RegionItem implements ClusterItem, Serializable {

    private static final long serialVersionUID = 20251109L; // 更新版本号

    // 1. 地图数据 (序列化修复)
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

    // 3. 【新增】详情页所需数据
    private String description;     // 笔记正文/描述
    private String noteType;        // 笔记类型 (e.g., "美食", "风景")
    private String locationName;    // 拍摄地点 (e.g., "北京市朝阳区...")

    // 构造函数 (用于测试)
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
    }

    // 兼容旧的构造函数 (用于 initClusterData)
    public RegionItem(LatLng latLng, String title) {
        this(latLng, "id_" + title, title, null,
                "测试用户", null, (int)(Math.random() * 100),
                "这是笔记的正文内容，用于测试显示。This is the note description.", // 默认正文
                "风景", // 默认类型
                "北京市朝阳区" // 默认地点
        );
    }

    // --- Getters (用于 Adapter 绑定数据) ---

    @Override
    public LatLng getPosition() {
        if (mLatLng == null) {
            mLatLng = new LatLng(mLatitude, mLongitude);
        }
        return mLatLng;
    }

    public String getNoteId() { return noteId; }
    public String getTitle() { return title; }
    public String getPhotoUrl() { return photoUrl; }
    public String getAuthorName() { return authorName; }
    public String getAuthorAvatarUrl() { return authorAvatarUrl; }
    public int getLikeCount() { return likeCount; }
    public void setLikeCount(int likeCount) { this.likeCount = Math.max(0, likeCount); }
    public boolean isLikedByCurrentUser() { return likedByCurrentUser; }
    public void setLikedByCurrentUser(boolean likedByCurrentUser) { this.likedByCurrentUser = likedByCurrentUser; }

    // 【新增】Getters
    public String getDescription() { return description; }
    public String getNoteType() { return noteType; }
    public String getLocationName() { return locationName; }
}