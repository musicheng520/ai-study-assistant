package com.msc.springai.dto.workflow.task;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateStudyTaskRequest {

    private Long documentId;

    private String title;

    private String description;

    private LocalDateTime dueDate;
}