package com.msc.springai.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class User {

    private Long id;

    private String email;

    private String passwordHash;

    private String displayName;

    private String role;

    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}