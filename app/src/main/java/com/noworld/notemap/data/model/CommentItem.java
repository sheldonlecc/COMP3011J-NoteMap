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
    private final String parentId;
    private final String replyToUserName;

    public CommentItem(String id, String userName, String content, String time, String avatarUrl) {
        this(id, userName, content, time, avatarUrl, null, null);
    }

    public CommentItem(String id, String userName, String content, String time, String avatarUrl, String parentId, String replyToUserName) {
        this.id = id;
        this.userName = userName;
        this.content = content;
        this.time = time;
        this.avatarUrl = avatarUrl;
        this.parentId = parentId;
        this.replyToUserName = replyToUserName;
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

    public String getParentId() {
        return parentId;
    }

    public String getReplyToUserName() {
        return replyToUserName;
    }

    public boolean isReply() {
        return parentId != null && !parentId.isEmpty();
    }
}
