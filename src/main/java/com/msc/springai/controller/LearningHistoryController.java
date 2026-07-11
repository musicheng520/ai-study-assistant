package com.msc.springai.controller;

import com.msc.springai.dto.learning.history.LearningHistoryListResponse;
import com.msc.springai.dto.learning.history.LearningHistorySummaryResponse;
import com.msc.springai.security.CurrentUserUtil;
import com.msc.springai.service.LearningHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class LearningHistoryController {

    private final LearningHistoryService learningHistoryService;

    @GetMapping("/api/learning-history/recent")
    public LearningHistoryListResponse getRecentActivities(
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) Integer offset
    ) {
        Long currentUserId =
                CurrentUserUtil.getCurrentUserId();

        return learningHistoryService.getRecentActivities(
                currentUserId,
                limit,
                offset
        );
    }

    @GetMapping("/api/courses/{courseId}/learning-history")
    public LearningHistoryListResponse getCourseActivities(
            @PathVariable Long courseId,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) Integer offset
    ) {
        Long currentUserId =
                CurrentUserUtil.getCurrentUserId();

        return learningHistoryService.getCourseActivities(
                currentUserId,
                courseId,
                limit,
                offset
        );
    }

    @GetMapping("/api/courses/{courseId}/learning-history/summary")
    public LearningHistorySummaryResponse getCourseActivitySummary(
            @PathVariable Long courseId
    ) {
        Long currentUserId =
                CurrentUserUtil.getCurrentUserId();

        return learningHistoryService.getCourseActivitySummary(
                currentUserId,
                courseId
        );
    }
}