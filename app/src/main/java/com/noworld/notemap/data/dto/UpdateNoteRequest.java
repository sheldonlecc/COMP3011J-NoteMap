package com.noworld.notemap.data.dto;

public class UpdateNoteRequest {
    // 这三个字段是可选的，想改哪个就传哪个
    public String title;
    public String description;
    public Boolean isPrivate;

    // 用于：仅修改可见性
    public UpdateNoteRequest(boolean isPrivate) {
        this.isPrivate = isPrivate;
    }

    // 用于：修改内容
    public UpdateNoteRequest(String title, String description) {
        this.title = title;
        this.description = description;
    }
}