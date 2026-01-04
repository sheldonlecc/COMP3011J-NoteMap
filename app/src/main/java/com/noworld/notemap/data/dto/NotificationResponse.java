package com.noworld.notemap.data.dto;

public class NotificationResponse {
    public String id;
    public String type;
    public String targetType;
    public String targetId;
    // When targetType=comment, backend may return the note ID for navigation
    public String noteId;
    public String message;
    public String time;
    public boolean read;
    public String actorName;
    public String actorAvatar;
}
