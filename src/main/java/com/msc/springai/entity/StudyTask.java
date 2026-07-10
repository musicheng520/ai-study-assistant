package com.msc.springai.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StudyTask {

    private Long id;

    private Long userId;

    private Long courseId;

    private Long documentId;

    private String title;

    private String description;

    private String status;

    private LocalDateTime dueDate;

    private String sourceType;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}