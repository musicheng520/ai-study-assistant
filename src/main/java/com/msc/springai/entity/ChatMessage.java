package com.msc.springai.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatMessage {

    private Long id;

    private Long sessionId;

    private Long userId;

    private Long courseId;

    private String role;

    private String content;

    private String workflowType;

    private Boolean noAnswer;

    private String modelName;

    private LocalDateTime createdAt;
}