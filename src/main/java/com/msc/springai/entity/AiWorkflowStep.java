package com.msc.springai.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AiWorkflowStep {

    private Long id;

    private Long workflowRunId;

    private String stepName;

    private String status;

    private LocalDateTime startedAt;

    private LocalDateTime completedAt;

    private String errorMessage;
}