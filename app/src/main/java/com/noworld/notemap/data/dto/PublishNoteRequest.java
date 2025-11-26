package com.noworld.notemap.data.dto;

import java.util.List;

public class PublishNoteRequest {
    public String title;
    public String description;
    public String type;
    public double latitude;
    public double longitude;
    public String locationName;
    public List<String> imageUrls;

    public PublishNoteRequest(String title, String description, String type,
                              double latitude, double longitude, String locationName,
                              List<String> imageUrls) {
        this.title = title;
        this.description = description;
        this.type = type;
        this.latitude = latitude;
        this.longitude = longitude;
        this.locationName = locationName;
        this.imageUrls = imageUrls;
    }
}
