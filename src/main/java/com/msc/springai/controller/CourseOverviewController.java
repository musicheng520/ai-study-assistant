package com.msc.springai.controller;

import com.msc.springai.dto.dashboard.CourseOverviewDtos.CourseOverviewResponse;
import com.msc.springai.security.CurrentUserUtil;
import com.msc.springai.service.CourseOverviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class CourseOverviewController {

    private final CourseOverviewService courseOverviewService;

    @GetMapping("/api/courses/{courseId}/overview")
    public CourseOverviewResponse getCourseOverview(
            @PathVariable Long courseId
    ) {
        Long currentUserId =
                CurrentUserUtil.getCurrentUserId();

        return courseOverviewService.getCourseOverview(
                currentUserId,
                courseId
        );
    }
}