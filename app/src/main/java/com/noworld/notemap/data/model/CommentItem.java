package com.noworld.notemap.data.model;

/**
 * UI 层使用的评论数据模型。
 */
public class CommentItem {
    private final String id;
    private final String userName;
    private final String content;
    private final String time;
    private final String avatarUrl;

    public CommentItem(String id, String userName, String content, String time, String avatarUrl) {
        this.id = id;
        this.userName = userName;
        this.content = content;
        this.time = time;
        this.avatarUrl = avatarUrl;
    }

    public String getId() {
        return id;
    }

    public String getUserName() {
        return userName;
    }

    public String getContent() {
        return content;
    }

    public String getTime() {
        return time;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }
}
