package com.noworld.notemap.data.dto;

public class RegisterRequest {
    public String email;
    public String password;
    public String nickname;

    public RegisterRequest(String email, String password, String nickname) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
    }
}
