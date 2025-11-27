package com.noworld.notemap.data.dto;

public class UpdateProfileRequest {
    public String nickname;
    public String avatarUrl;

    public UpdateProfileRequest(String nickname, String avatarUrl) {
        this.nickname = nickname;
        this.avatarUrl = avatarUrl;
    }
}