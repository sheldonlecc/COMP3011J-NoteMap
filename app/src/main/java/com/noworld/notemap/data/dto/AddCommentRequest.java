package com.noworld.notemap.data.dto;

/**
 * Request body for posting a new comment.
 */
public class AddCommentRequest {
    public String content;
    public String parentId;

    public AddCommentRequest(String content) {
        this.content = content;
    }

    public AddCommentRequest(String content, String parentId) {
        this.content = content;
        this.parentId = parentId;
    }
}
