package com.noworld.notemap.data.dto;

import com.google.gson.annotations.SerializedName;

public class UpdateProfileResponse {
    public String message;
    public UserData user;

    public static class UserData {
        public String uid;
        public String username;

        @SerializedName(value = "avatarUrl", alternate = {"avatar_url", "avatar"})
        public String avatarUrl;
    }
}