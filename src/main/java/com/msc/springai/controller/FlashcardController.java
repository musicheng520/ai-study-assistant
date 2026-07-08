package com.msc.springai.controller;

import com.msc.springai.dto.learning.request.FlashcardGenerateRequest;
import com.msc.springai.dto.learning.request.SaveDraftRequest;
import com.msc.springai.dto.learning.response.FlashcardGenerateResponse;
import com.msc.springai.dto.learning.response.FlashcardSaveResponse;
import com.msc.springai.dto.learning.response.SavedFlashcardResponse;
import com.msc.springai.security.CurrentUserUtil;
import com.msc.springai.service.FlashcardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class FlashcardController {

    private final FlashcardService flashcardService;

    @PostMapping("/api/courses/{courseId}/flashcards/generate")
    public FlashcardGenerateResponse generateCourseFlashcards(
            @PathVariable Long courseId,
            @RequestBody(required = false) FlashcardGenerateRequest request
    ) {
        Long currentUserId = CurrentUserUtil.getCurrentUserId();

        return flashcardService.generateCourseFlashcards(
                currentUserId,
                courseId,
                request
        );
    }

    @PostMapping("/api/documents/{documentId}/flashcards/generate")
    public FlashcardGenerateResponse generateDocumentFlashcards(
            @PathVariable Long documentId,
            @RequestBody(required = false) FlashcardGenerateRequest request
    ) {
        Long currentUserId = CurrentUserUtil.getCurrentUserId();

        return flashcardService.generateDocumentFlashcards(
                currentUserId,
                documentId,
                request
        );
    }

    @PostMapping("/api/flashcards/save")
    public FlashcardSaveResponse saveFlashcards(
            @RequestBody SaveDraftRequest request
    ) {
        Long currentUserId = CurrentUserUtil.getCurrentUserId();

        return flashcardService.saveFlashcards(
                currentUserId,
                request
        );
    }

    @GetMapping("/api/courses/{courseId}/flashcards")
    public List<SavedFlashcardResponse> getCourseFlashcards(
            @PathVariable Long courseId
    ) {
        Long currentUserId = CurrentUserUtil.getCurrentUserId();

        return flashcardService.getCourseFlashcards(
                currentUserId,
                courseId
        );
    }

    @DeleteMapping("/api/flashcards/{flashcardId}")
    public Map<String, Object> deleteFlashcard(
            @PathVariable Long flashcardId
    ) {
        Long currentUserId = CurrentUserUtil.getCurrentUserId();

        flashcardService.deleteFlashcard(
                currentUserId,
                flashcardId
        );

        return Map.of(
                "deleted", true,
                "flashcardId", flashcardId
        );
    }
}