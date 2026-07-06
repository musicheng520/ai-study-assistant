package com.msc.springai.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DocumentProcessingJob {

    private Long id;
    private Long userId;
    private Long courseId;
    private Long documentId;

    private String status;
    private String step;
    private String errorMessage;
    private Integer retryCount;

    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
}