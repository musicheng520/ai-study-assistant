package com.msc.springai.dto.workflow.task;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StudyTaskResponse {

    private Long id;

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