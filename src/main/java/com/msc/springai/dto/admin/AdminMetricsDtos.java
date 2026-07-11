package com.msc.springai.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

public final class AdminMetricsDtos {

    private AdminMetricsDtos() {
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AiMetricsResponse {

        private Integer days;

        private Long totalRequests;

        private Long successCount;

        private Long failedCount;

        private Double successRate;

        private Long totalTokens;

        private Double averageLatencyMs;

        private Long maxLatencyMs;

        private Double averageRetrievedChunkCount;

        private List<WorkflowUsageItem> workflowUsage;

        private List<RecentAiFailureItem> recentFailures;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CacheMetricsResponse {

        private Integer days;

        private Long ragRequestCount;

        private Long cacheHitCount;

        private Long cacheMissCount;

        private Double cacheHitRate;

        private Double averageCacheHitLatencyMs;

        private Double averageCacheMissLatencyMs;

        private Double averageRetrievedChunkCount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AiRequestLogResponse {

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

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WorkflowRunLogResponse {

        private Long id;

        private Long userId;

        private Long courseId;

        private String workflowType;

        private String status;

        private String errorMessage;

        private LocalDateTime startedAt;

        private LocalDateTime completedAt;

        private Long durationMs;

        private Integer stepCount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WorkflowUsageItem {

        private String workflowType;

        private Long requestCount;

        private Long failedCount;

        private Long totalTokens;

        private Double averageLatencyMs;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentAiFailureItem {

        private Long id;

        private Long userId;

        private Long courseId;

        private String workflowType;

        private String modelName;

        private String errorType;

        private String errorMessage;

        private LocalDateTime createdAt;
    }

    /*
     * Mapper aggregate row.
     * 这个类只在 Service 内部组装 response 用。
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AiMetricsSummaryRow {

        private Long totalRequests;

        private Long successCount;

        private Long failedCount;

        private Long totalTokens;

        private Double averageLatencyMs;

        private Long maxLatencyMs;

        private Double averageRetrievedChunkCount;
    }

    /*
     * Mapper aggregate row.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CacheMetricsSummaryRow {

        private Long ragRequestCount;

        private Long cacheHitCount;

        private Long cacheMissCount;

        private Double averageCacheHitLatencyMs;

        private Double averageCacheMissLatencyMs;

        private Double averageRetrievedChunkCount;
    }
}