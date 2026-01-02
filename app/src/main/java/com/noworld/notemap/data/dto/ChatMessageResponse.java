package com.noworld.notemap.data.dto;

public class ChatMessageResponse {
    public String id;
    public String fromUserId;
    public String toUserId;
    public String content;
    public String mediaUrl;
    /**
     * Optional values: text / image / video. Defaults to text when null.
     */
    public String mediaType;
    public long createdAt;
}
