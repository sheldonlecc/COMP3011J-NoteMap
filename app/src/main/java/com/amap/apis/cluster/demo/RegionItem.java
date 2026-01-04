package com.amap.apis.cluster.demo;

import com.amap.api.maps.model.LatLng;
import com.amap.apis.cluster.ClusterItem;
import java.io.Serializable;
import java.util.List;

public class RegionItem implements ClusterItem, Serializable {

    private static final long serialVersionUID = 20251109L;

    // 1. Map data
    private transient LatLng mLatLng;
    private double mLatitude;
    private double mLongitude;

    // 2. Note data
    private String noteId;
    private String title;
    private String photoUrl;
    private String authorName;
    private String authorAvatarUrl;
    private int likeCount;
    private boolean likedByCurrentUser;

    // 3. Detail view data
    private String description;     // Note body
    private String noteType;        // Note category
    private String locationName;    // Location name
    private List<String> imageUrls; // Image list

    // 4. Added: publish time field (fixes crash)
    private String createTime;

    private boolean isPrivate;

    private String authorId;



    // Constructor
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
        // Default placeholder, set by MainActivity#setCreateTime
        this.createTime = "2023-11-27";
    }

    // Legacy constructor
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

    // Added getter and setter
    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    // Other getters
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

    // Added in RegionItem.java
    public void setAuthorAvatarUrl(String authorAvatarUrl) {
        this.authorAvatarUrl = authorAvatarUrl;
    }
    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    // Additional getter and setter
    public boolean isPrivate() {
        return isPrivate;
    }

    public void setPrivate(boolean isPrivate) {
        this.isPrivate = isPrivate;
    }

    public String getAuthorId() {
        return authorId;
    }

    // Add setAuthorId() for JSON parsing
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
