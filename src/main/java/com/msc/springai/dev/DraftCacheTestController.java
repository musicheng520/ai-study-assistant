package com.msc.springai.dev;

import com.msc.springai.dto.cache.DraftKeyInfo;
import com.msc.springai.service.DraftCacheService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/dev/draft-cache")
@RequiredArgsConstructor
public class DraftCacheTestController {

    private final DraftCacheService draftCacheService;

    @PostMapping("/summary")
    public Map<String, Object> saveSummaryDraft() {
        String draftKey = draftCacheService.buildSummaryDraftKey(
                1L,
                3L,
                "COURSE",
                Map.of(
                        "topK", 8,
                        "style", "REVISION"
                )
        );

        TestDraftValue value = new TestDraftValue(
                "Course Summary",
                "This is a test summary draft."
        );

        draftCacheService.saveDraft(draftKey, value);

        return Map.of(
                "draftKey", draftKey,
                "value", value
        );
    }

    @PostMapping("/quiz")
    public Map<String, Object> saveQuizDraft() {
        String draftKey = draftCacheService.buildQuizDraftKey(
                1L,
                3L,
                "DOCUMENT",
                Map.of(
                        "documentId", 10L,
                        "mcqCount", 5,
                        "shortAnswerCount", 3,
                        "difficulty", "MEDIUM"
                )
        );

        TestDraftValue value = new TestDraftValue(
                "Document Quiz",
                "This is a test quiz draft."
        );

        draftCacheService.saveDraft(draftKey, value);

        return Map.of(
                "draftKey", draftKey,
                "value", value
        );
    }

    @PostMapping("/flashcard")
    public Map<String, Object> saveFlashcardDraft() {
        String draftKey = draftCacheService.buildFlashcardDraftKey(
                1L,
                3L,
                "COURSE",
                Map.of(
                        "count", 10,
                        "difficulty", "MEDIUM"
                )
        );

        TestDraftValue value = new TestDraftValue(
                "Course Flashcards",
                "This is a test flashcard draft."
        );

        draftCacheService.saveDraft(draftKey, value);

        return Map.of(
                "draftKey", draftKey,
                "value", value
        );
    }

    @GetMapping
    public TestDraftValue getDraft(@RequestParam String draftKey) {
        return draftCacheService.getDraft(
                draftKey,
                TestDraftValue.class
        );
    }

    @DeleteMapping
    public Map<String, Object> deleteDraft(@RequestParam String draftKey) {
        draftCacheService.deleteDraft(draftKey);

        return Map.of(
                "deleted", true,
                "draftKey", draftKey
        );
    }

    @GetMapping("/parse")
    public DraftKeyInfo parseDraftKey(@RequestParam String draftKey) {
        return draftCacheService.parseDraftKey(draftKey);
    }

    @GetMapping("/owner")
    public Map<String, Object> validateOwner(
            @RequestParam String draftKey,
            @RequestParam Long currentUserId
    ) {
        draftCacheService.validateDraftOwner(
                draftKey,
                currentUserId
        );

        return Map.of(
                "valid", true,
                "draftKey", draftKey,
                "currentUserId", currentUserId
        );
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TestDraftValue {

        private String title;

        private String content;
    }
}