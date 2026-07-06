package com.msc.springai.dto.auth;

import com.msc.springai.entity.User;
import lombok.Data;

@Data
public class CurrentUserResponse {

    private Long id;
    private String email;
    private String displayName;
    private String role;
    private String status;

    public static CurrentUserResponse from(User user) {
        CurrentUserResponse response = new CurrentUserResponse();
        response.setId(user.getId());
        response.setEmail(user.getEmail());
        response.setDisplayName(user.getDisplayName());
        response.setRole(user.getRole());
        response.setStatus(user.getStatus());
        return response;
    }
}