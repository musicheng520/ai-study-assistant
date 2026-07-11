package com.msc.springai.service;

import com.msc.springai.dto.admin.AdminMetricsDtos.AiMetricsResponse;
import com.msc.springai.dto.admin.AdminMetricsDtos.AiMetricsSummaryRow;
import com.msc.springai.dto.admin.AdminMetricsDtos.AiRequestLogResponse;
import com.msc.springai.dto.admin.AdminMetricsDtos.CacheMetricsResponse;
import com.msc.springai.dto.admin.AdminMetricsDtos.CacheMetricsSummaryRow;
import com.msc.springai.dto.admin.AdminMetricsDtos.RecentAiFailureItem;
import com.msc.springai.dto.admin.AdminMetricsDtos.WorkflowRunLogResponse;
import com.msc.springai.dto.admin.AdminMetricsDtos.WorkflowUsageItem;
import com.msc.springai.exception.BusinessException;
import com.msc.springai.mapper.AdminMetricsMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AdminMetricsService {

    private static final int DEFAULT_DAYS = 7;
    private static final int MIN_DAYS = 1;
    private static final int MAX_DAYS = 90;

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;

    private static final Set<String> VALID_WORKFLOW_TYPES = Set.of(
            "RAG_QA",
            "SUMMARY",
            "QUIZ",
            "FLASHCARD",
            "SHORT_ANSWER_GRADING",
            "ASSIGNMENT_ANALYSIS",
            "RUBRIC_ANALYSIS",
            "CHECKLIST_GENERATION",
            "REVISION_PACK",
            "COORDINATOR"
    );

    private static final Set<String> VALID_WORKFLOW_RUN_TYPES = Set.of(
            "RAG_QA",
            "SUMMARY",
            "QUIZ",
            "FLASHCARD",
            "ASSIGNMENT_ANALYSIS",
            "RUBRIC_ANALYSIS",
            "REVISION_PLAN",
            "CHECKLIST",
            "UNKNOWN"
    );

    private static final Set<String> VALID_WORKFLOW_STATUS = Set.of(
            "RUNNING",
            "SUCCESS",
            "FAILED"
    );

    private final AdminMetricsMapper adminMetricsMapper;

    public AiMetricsResponse getAiMetrics(
            Long currentUserId,
            Integer days
    ) {
        validateAdmin(currentUserId);

        int normalizedDays =
                normalizeDays(days);

        AiMetricsSummaryRow summary =
                adminMetricsMapper.findAiMetricsSummary(
                        normalizedDays
                );

        List<WorkflowUsageItem> workflowUsage =
                adminMetricsMapper.findWorkflowUsage(
                        normalizedDays,
                        20
                );

        List<RecentAiFailureItem> recentFailures =
                adminMetricsMapper.findRecentFailures(
                        10
                );

        long totalRequests =
                safeLong(summary.getTotalRequests());

        long successCount =
                safeLong(summary.getSuccessCount());

        double successRate =
                rate(
                        successCount,
                        totalRequests
                );

        return new AiMetricsResponse(
                normalizedDays,
                totalRequests,
                successCount,
                safeLong(summary.getFailedCount()),
                successRate,
                safeLong(summary.getTotalTokens()),
                safeDouble(summary.getAverageLatencyMs()),
                safeLong(summary.getMaxLatencyMs()),
                safeDouble(summary.getAverageRetrievedChunkCount()),
                workflowUsage,
                recentFailures
        );
    }

    public CacheMetricsResponse getCacheMetrics(
            Long currentUserId,
            Integer days
    ) {
        validateAdmin(currentUserId);

        int normalizedDays =
                normalizeDays(days);

        CacheMetricsSummaryRow summary =
                adminMetricsMapper.findCacheMetricsSummary(
                        normalizedDays
                );

        long ragRequestCount =
                safeLong(summary.getRagRequestCount());

        long cacheHitCount =
                safeLong(summary.getCacheHitCount());

        return new CacheMetricsResponse(
                normalizedDays,
                ragRequestCount,
                cacheHitCount,
                safeLong(summary.getCacheMissCount()),
                rate(cacheHitCount, ragRequestCount),
                safeDouble(summary.getAverageCacheHitLatencyMs()),
                safeDouble(summary.getAverageCacheMissLatencyMs()),
                safeDouble(summary.getAverageRetrievedChunkCount())
        );
    }

    public List<AiRequestLogResponse> getAiRequestLogs(
            Long currentUserId,
            String workflowType,
            Boolean onlyFailures,
            Integer limit,
            Integer offset
    ) {
        validateAdmin(currentUserId);

        String normalizedWorkflowType =
                normalizeOptionalWorkflowType(
                        workflowType,
                        VALID_WORKFLOW_TYPES
                );

        boolean normalizedOnlyFailures =
                Boolean.TRUE.equals(onlyFailures);

        return adminMetricsMapper.findAiRequestLogs(
                normalizedWorkflowType,
                normalizedOnlyFailures,
                normalizeLimit(limit),
                normalizeOffset(offset)
        );
    }

    public List<WorkflowRunLogResponse> getWorkflowRuns(
            Long currentUserId,
            String status,
            String workflowType,
            Integer limit,
            Integer offset
    ) {
        validateAdmin(currentUserId);

        String normalizedStatus =
                normalizeOptionalStatus(status);

        String normalizedWorkflowType =
                normalizeOptionalWorkflowType(
                        workflowType,
                        VALID_WORKFLOW_RUN_TYPES
                );

        return adminMetricsMapper.findWorkflowRuns(
                normalizedStatus,
                normalizedWorkflowType,
                normalizeLimit(limit),
                normalizeOffset(offset)
        );
    }

    private void validateAdmin(
            Long currentUserId
    ) {
        if (currentUserId == null) {
            throw new BusinessException(
                    "UNAUTHORIZED",
                    "Current user is required."
            );
        }

        int adminCount =
                adminMetricsMapper.countActiveAdmin(
                        currentUserId
                );

        if (adminCount <= 0) {
            throw new BusinessException(
                    "FORBIDDEN",
                    "Admin access is required."
            );
        }
    }

    private int normalizeDays(
            Integer days
    ) {
        if (days == null) {
            return DEFAULT_DAYS;
        }

        if (days < MIN_DAYS) {
            return MIN_DAYS;
        }

        return Math.min(
                days,
                MAX_DAYS
        );
    }

    private int normalizeLimit(
            Integer limit
    ) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }

        if (limit < 1) {
            return 1;
        }

        return Math.min(
                limit,
                MAX_LIMIT
        );
    }

    private int normalizeOffset(
            Integer offset
    ) {
        if (offset == null || offset < 0) {
            return 0;
        }

        return offset;
    }

    private String normalizeOptionalStatus(
            String status
    ) {
        if (status == null || status.isBlank()) {
            return null;
        }

        String normalized =
                status
                        .trim()
                        .toUpperCase();

        if (!VALID_WORKFLOW_STATUS.contains(normalized)) {
            throw new BusinessException(
                    "INVALID_WORKFLOW_STATUS",
                    "Unsupported workflow status."
            );
        }

        return normalized;
    }

    private String normalizeOptionalWorkflowType(
            String workflowType,
            Set<String> allowedTypes
    ) {
        if (workflowType == null || workflowType.isBlank()) {
            return null;
        }

        String normalized =
                workflowType
                        .trim()
                        .toUpperCase();

        if (!allowedTypes.contains(normalized)) {
            throw new BusinessException(
                    "INVALID_WORKFLOW_TYPE",
                    "Unsupported workflow type."
            );
        }

        return normalized;
    }

    private long safeLong(
            Long value
    ) {
        return value == null
                ? 0L
                : Math.max(value, 0L);
    }

    private double safeDouble(
            Double value
    ) {
        if (value == null) {
            return 0.0;
        }

        if (Double.isNaN(value)
                || Double.isInfinite(value)) {
            return 0.0;
        }

        return Math.max(value, 0.0);
    }

    private double rate(
            long numerator,
            long denominator
    ) {
        if (denominator <= 0) {
            return 0.0;
        }

        double result =
                numerator * 100.0 / denominator;

        return Math.round(result * 100.0) / 100.0;
    }
}