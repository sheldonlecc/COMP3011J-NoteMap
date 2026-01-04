package com.noworld.notemap.data.dto;

public class UpdateNoteRequest {
    // Optional fields: send whichever you want to update
    public String title;
    public String description;
    public Boolean isPrivate;

    // For updating visibility only
    public UpdateNoteRequest(boolean isPrivate) {
        this.isPrivate = isPrivate;
    }

    // For updating content
    public UpdateNoteRequest(String title, String description) {
        this.title = title;
        this.description = description;
    }
}
