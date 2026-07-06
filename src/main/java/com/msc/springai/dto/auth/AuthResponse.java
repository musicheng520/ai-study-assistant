package com.msc.springai.dto.auth;

import com.msc.springai.entity.User;
import lombok.Data;

@Data
public class AuthResponse {

    private String token;
    private Long userId;
    private String email;
    private String displayName;
    private String role;

    public static AuthResponse from(User user, String token) {
        AuthResponse response = new AuthResponse();
        response.setToken(token);
        response.setUserId(user.getId());
        response.setEmail(user.getEmail());
        response.setDisplayName(user.getDisplayName());
        response.setRole(user.getRole());
        return response;
    }
}