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
    private final int likeCount;
    private final boolean liked;
    private final boolean isMoreIndicator;
    private final int remainingCount;

    public CommentItem(String id, String userName, String content, String time, String avatarUrl) {
        this(id, userName, content, time, avatarUrl, null, null, 0, false, false, 0);
    }

    public CommentItem(String id, String userName, String content, String time, String avatarUrl,
                       String parentId, String replyToUserName, int likeCount, boolean liked) {
        this(id, userName, content, time, avatarUrl, parentId, replyToUserName, likeCount, liked, false, 0);
    }

    public CommentItem(String id, String userName, String content, String time, String avatarUrl,
                       String parentId, String replyToUserName, int likeCount, boolean liked,
                       boolean isMoreIndicator, int remainingCount) {
        this.id = id;
        this.userName = userName;
        this.content = content;
        this.time = time;
        this.avatarUrl = avatarUrl;
        this.parentId = parentId;
        this.replyToUserName = replyToUserName;
        this.likeCount = Math.max(0, likeCount);
        this.liked = liked;
        this.isMoreIndicator = isMoreIndicator;
        this.remainingCount = remainingCount;
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

    public int getLikeCount() {
        return likeCount;
    }

    public boolean isLiked() {
        return liked;
    }

    public boolean isMoreIndicator() {
        return isMoreIndicator;
    }

    public int getRemainingCount() {
        return remainingCount;
    }
}
