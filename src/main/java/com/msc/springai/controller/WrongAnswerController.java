package com.msc.springai.controller;

import com.msc.springai.dto.learning.response.CourseWrongAnswersResponse;
import com.msc.springai.dto.learning.response.WrongAnswerResolvedResponse;
import com.msc.springai.security.CurrentUserUtil;
import com.msc.springai.service.WrongAnswerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class WrongAnswerController {

    private final WrongAnswerService wrongAnswerService;

    @GetMapping("/api/courses/{courseId}/wrong-answers")
    public CourseWrongAnswersResponse getCourseWrongAnswers(
            @PathVariable Long courseId,
            @RequestParam(required = false) Boolean resolved,
            @RequestParam(required = false) String topic
    ) {
        Long currentUserId = CurrentUserUtil.getCurrentUserId();

        return wrongAnswerService.getCourseWrongAnswers(
                currentUserId,
                courseId,
                resolved,
                topic
        );
    }

    @PatchMapping("/api/wrong-answers/{wrongAnswerId}/resolved")
    public WrongAnswerResolvedResponse markResolved(
            @PathVariable Long wrongAnswerId
    ) {
        Long currentUserId = CurrentUserUtil.getCurrentUserId();

        return wrongAnswerService.markResolved(
                currentUserId,
                wrongAnswerId
        );
    }

    @DeleteMapping("/api/wrong-answers/{wrongAnswerId}")
    public Map<String, Object> deleteWrongAnswer(
            @PathVariable Long wrongAnswerId
    ) {
        Long currentUserId = CurrentUserUtil.getCurrentUserId();

        wrongAnswerService.deleteWrongAnswer(
                currentUserId,
                wrongAnswerId
        );

        return Map.of(
                "deleted", true,
                "wrongAnswerId", wrongAnswerId
        );
    }
}