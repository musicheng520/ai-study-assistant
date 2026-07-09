package com.msc.springai.controller;

import com.msc.springai.dto.learning.response.CourseWeakTopicsResponse;
import com.msc.springai.security.CurrentUserUtil;
import com.msc.springai.service.ProgressService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class ProgressController {

    private final ProgressService progressService;

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
}