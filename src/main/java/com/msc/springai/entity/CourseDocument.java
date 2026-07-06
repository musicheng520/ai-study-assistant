package com.msc.springai.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CourseDocument {

    private Long id;
    private Long userId;
    private Long courseId;

    private String originalFileName;
    private String storedFilePath;
    private String fileType;
    private String documentType;
    private Long fileSize;

    private String status;
    private String errorMessage;
    private Integer version;
    private Integer totalPages;
    private Integer chunkCount;

    private LocalDateTime processedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}