package com.noworld.notemap.data.dto;

/**
 * 请求体：发表新评论。
 */
public class AddCommentRequest {
    public String content;

    public AddCommentRequest(String content) {
        this.content = content;
    }
}
