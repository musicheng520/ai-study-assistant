package com.msc.springai.controller;

import com.msc.springai.dto.learning.request.QuizGenerateRequest;
import com.msc.springai.dto.learning.request.SaveDraftRequest;
import com.msc.springai.dto.learning.response.QuizDetailResponse;
import com.msc.springai.dto.learning.response.QuizGenerateResponse;
import com.msc.springai.dto.learning.response.QuizSaveResponse;
import com.msc.springai.dto.learning.response.SavedQuizResponse;
import com.msc.springai.security.CurrentUserUtil;
import com.msc.springai.service.QuizService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;

    @PostMapping("/api/courses/{courseId}/quiz/generate")
    public QuizGenerateResponse generateCourseQuiz(
            @PathVariable Long courseId,
            @RequestBody(required = false) QuizGenerateRequest request
    ) {
        Long currentUserId = CurrentUserUtil.getCurrentUserId();

        return quizService.generateCourseQuiz(
                currentUserId,
                courseId,
                request
        );
    }

    @PostMapping("/api/documents/{documentId}/quiz/generate")
    public QuizGenerateResponse generateDocumentQuiz(
            @PathVariable Long documentId,
            @RequestBody(required = false) QuizGenerateRequest request
    ) {
        Long currentUserId = CurrentUserUtil.getCurrentUserId();

        return quizService.generateDocumentQuiz(
                currentUserId,
                documentId,
                request
        );
    }

    @PostMapping("/api/quiz/save")
    public QuizSaveResponse saveQuiz(
            @RequestBody SaveDraftRequest request
    ) {
        Long currentUserId = CurrentUserUtil.getCurrentUserId();

        return quizService.saveQuiz(
                currentUserId,
                request
        );
    }

    @GetMapping("/api/courses/{courseId}/quizzes")
    public List<SavedQuizResponse> getCourseQuizzes(
            @PathVariable Long courseId
    ) {
        Long currentUserId = CurrentUserUtil.getCurrentUserId();

        return quizService.getCourseQuizzes(
                currentUserId,
                courseId
        );
    }

    @GetMapping("/api/quizzes/{quizId}")
    public QuizDetailResponse getQuizDetail(
            @PathVariable Long quizId
    ) {
        Long currentUserId = CurrentUserUtil.getCurrentUserId();

        return quizService.getQuizDetail(
                currentUserId,
                quizId
        );
    }

    @DeleteMapping("/api/quizzes/{quizId}")
    public Map<String, Object> deleteQuiz(
            @PathVariable Long quizId
    ) {
        Long currentUserId = CurrentUserUtil.getCurrentUserId();

        quizService.deleteQuiz(
                currentUserId,
                quizId
        );

        return Map.of(
                "deleted", true,
                "quizId", quizId
        );
    }
}