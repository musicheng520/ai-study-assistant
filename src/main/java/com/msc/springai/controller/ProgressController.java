package com.msc.springai.controller;

import com.msc.springai.dto.learning.response.*;
import com.msc.springai.security.CurrentUserUtil;
import com.msc.springai.service.ProgressService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class ProgressController {

    private final ProgressService progressService;

    @GetMapping("/api/progress/overview")
    public UserProgressOverviewResponse getUserOverview() {
        Long currentUserId = CurrentUserUtil.getCurrentUserId();

        return progressService.getUserOverview(currentUserId);
    }

    @GetMapping("/api/courses/{courseId}/progress")
    public CourseProgressResponse getCourseProgress(
            @PathVariable Long courseId
    ) {
        Long currentUserId = CurrentUserUtil.getCurrentUserId();

        return progressService.getCourseProgress(
                currentUserId,
                courseId
        );
    }

    @GetMapping("/api/courses/{courseId}/progress/wrong-topics")
    public CourseWeakTopicsResponse getCourseWeakTopics(
            @PathVariable Long courseId
    ) {
        Long currentUserId = CurrentUserUtil.getCurrentUserId();

        return progressService.getCourseWeakTopics(
                currentUserId,
                courseId
        );
    }

    @GetMapping("/api/courses/{courseId}/progress/recommendations")
    public CourseReviewRecommendationsResponse getCourseRecommendations(
            @PathVariable Long courseId
    ) {
        Long currentUserId = CurrentUserUtil.getCurrentUserId();

        return progressService.getCourseRecommendations(
                currentUserId,
                courseId
        );
    }

    @GetMapping("/api/courses/{courseId}/activity")
    public CourseActivityResponse getCourseActivity(
            @PathVariable Long courseId
    ) {
        Long currentUserId = CurrentUserUtil.getCurrentUserId();

        return progressService.getCourseActivity(
                currentUserId,
                courseId
        );
    }

    @GetMapping("/api/progress/streak")
    public StudyStreakResponse getStudyStreak() {
        Long currentUserId = CurrentUserUtil.getCurrentUserId();

        return progressService.getStudyStreak(currentUserId);
    }

}