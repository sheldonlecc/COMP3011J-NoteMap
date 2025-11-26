package com.noworld.notemap.data.dto;

public class LoginResponse {
    private String token;
    private UserDto user;

    public String getToken() {
        return token;
    }

    public UserDto getUser() {
        return user;
    }

    public static class UserDto {
        public String uid;
        public String username;
        public String avatarUrl;
    }
}
