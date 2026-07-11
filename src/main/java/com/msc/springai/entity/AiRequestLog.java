package com.msc.springai.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AiRequestLog {

    private Long id;

    private Long userId;

    private Long courseId;

    private String workflowType;

    private String modelName;

    private Integer promptTokens;

    private Integer completionTokens;

    private Integer totalTokens;

    private Long latencyMs;

    private Boolean cacheHit;

    private Integer retrievedChunkCount;

    private String errorType;

    private String errorMessage;

    private LocalDateTime createdAt;
}