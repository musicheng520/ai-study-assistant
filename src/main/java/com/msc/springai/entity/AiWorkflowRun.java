package com.msc.springai.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AiWorkflowRun {

    private Long id;

    private Long userId;

    private Long courseId;

    private String workflowType;

    private String status;

    private String inputJson;

    private String outputJson;

    private String errorMessage;

    private LocalDateTime startedAt;

    private LocalDateTime completedAt;
}