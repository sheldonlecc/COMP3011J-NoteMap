package com.noworld.notemap.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amap.api.maps.model.LatLng;
import com.amap.apis.cluster.demo.RegionItem;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.GeoPoint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private Timestamp timestamp;

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

    public static MapNote fromSnapshot(@NonNull DocumentSnapshot snapshot) {
        MapNote note = new MapNote();
        note.id = snapshot.getId();
        note.title = snapshot.getString(FIELD_TITLE);
        note.description = snapshot.getString(FIELD_DESCRIPTION);
        note.type = snapshot.getString(FIELD_TYPE);
        note.locationName = snapshot.getString(FIELD_LOCATION_NAME);
        note.authorId = snapshot.getString(FIELD_AUTHOR_ID);
        note.authorName = snapshot.getString(FIELD_AUTHOR_NAME);
        note.authorAvatarUrl = snapshot.getString(FIELD_AUTHOR_AVATAR);
        Long like = snapshot.getLong(FIELD_LIKE_COUNT);
        note.likeCount = like != null ? like.intValue() : 0;
        note.timestamp = snapshot.getTimestamp(FIELD_TIMESTAMP);

        GeoPoint geoPoint = snapshot.getGeoPoint(FIELD_GEO_POINT);
        if (geoPoint != null) {
            note.latitude = geoPoint.getLatitude();
            note.longitude = geoPoint.getLongitude();
        }

        List<String> urls = (List<String>) snapshot.get(FIELD_IMAGE_URLS);
        note.imageUrls = urls != null ? urls : new ArrayList<>();
        return note;
    }

    public Map<String, Object> toFirestoreMap() {
        if (imageUrls == null) {
            imageUrls = new ArrayList<>();
        }
        Map<String, Object> map = new HashMap<>();
        map.put(FIELD_TITLE, title);
        map.put(FIELD_DESCRIPTION, description);
        map.put(FIELD_TYPE, type);
        map.put(FIELD_LOCATION_NAME, locationName);
        map.put(FIELD_AUTHOR_ID, authorId);
        map.put(FIELD_AUTHOR_NAME, authorName);
        map.put(FIELD_AUTHOR_AVATAR, authorAvatarUrl);
        map.put(FIELD_LIKE_COUNT, likeCount);
        map.put(FIELD_GEO_POINT, new GeoPoint(latitude, longitude));
        map.put(FIELD_IMAGE_URLS, imageUrls);
        map.put(FIELD_TIMESTAMP, com.google.firebase.firestore.FieldValue.serverTimestamp());
        return map;
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
}

