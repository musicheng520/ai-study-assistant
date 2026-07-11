package com.msc.springai.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LearningHistory {

    private Long id;

    private Long userId;

    private Long courseId;

    private String eventType;

    private String targetType;

    private Long targetId;

    private String topic;

    private LocalDateTime createdAt;
}