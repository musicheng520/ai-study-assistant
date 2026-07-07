package com.msc.springai.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatSession {

    private Long id;

    private Long userId;

    private Long courseId;

    private String title;

    private String scopeType;

    private Long documentId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}