package com.msc.springai.controller;

import com.msc.springai.dto.admin.AdminMetricsDtos.AiMetricsResponse;
import com.msc.springai.dto.admin.AdminMetricsDtos.AiRequestLogResponse;
import com.msc.springai.dto.admin.AdminMetricsDtos.CacheMetricsResponse;
import com.msc.springai.dto.admin.AdminMetricsDtos.WorkflowRunLogResponse;
import com.msc.springai.security.CurrentUserUtil;
import com.msc.springai.service.AdminMetricsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminMetricsController {

    private final AdminMetricsService adminMetricsService;

    @GetMapping("/metrics/ai")
    public AiMetricsResponse getAiMetrics(
            @RequestParam(required = false) Integer days
    ) {
        Long currentUserId =
                CurrentUserUtil.getCurrentUserId();

        return adminMetricsService.getAiMetrics(
                currentUserId,
                days
        );
    }

    @GetMapping("/metrics/cache")
    public CacheMetricsResponse getCacheMetrics(
            @RequestParam(required = false) Integer days
    ) {
        Long currentUserId =
                CurrentUserUtil.getCurrentUserId();

        return adminMetricsService.getCacheMetrics(
                currentUserId,
                days
        );
    }

    @GetMapping("/logs/ai-requests")
    public List<AiRequestLogResponse> getAiRequestLogs(
            @RequestParam(required = false) String workflowType,
            @RequestParam(required = false) Boolean onlyFailures,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) Integer offset
    ) {
        Long currentUserId =
                CurrentUserUtil.getCurrentUserId();

        return adminMetricsService.getAiRequestLogs(
                currentUserId,
                workflowType,
                onlyFailures,
                limit,
                offset
        );
    }

    @GetMapping("/workflows")
    public List<WorkflowRunLogResponse> getWorkflowRuns(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String workflowType,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) Integer offset
    ) {
        Long currentUserId =
                CurrentUserUtil.getCurrentUserId();

        return adminMetricsService.getWorkflowRuns(
                currentUserId,
                status,
                workflowType,
                limit,
                offset
        );
    }
}