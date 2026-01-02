package com.noworld.notemap.data.dto;

public class SendMessageRequest {
    public String content;
    public String mediaUrl;
    public String mediaType;

    public SendMessageRequest(String content, String mediaUrl, String mediaType) {
        this.content = content;
        this.mediaUrl = mediaUrl;
        this.mediaType = mediaType;
    }
}
