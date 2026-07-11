package com.msc.springai.dto.learning.history;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LearningHistoryItemResponse {

    private Long id;

    private Long courseId;

    private String eventType;

    private String targetType;

    private Long targetId;

    private String topic;

    private String title;

    private String description;

    private String iconType;

    private LocalDateTime createdAt;
}