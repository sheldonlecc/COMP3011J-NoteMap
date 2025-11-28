package com.noworld.notemap.data.dto;

public class NotificationResponse {
    public String id;
    public String type;
    public String targetType;
    public String targetId;
    // 当 targetType=comment 时，后端可返回对应的笔记 ID，便于跳转
    public String noteId;
    public String message;
    public String time;
    public boolean read;
    public String actorName;
    public String actorAvatar;
}
