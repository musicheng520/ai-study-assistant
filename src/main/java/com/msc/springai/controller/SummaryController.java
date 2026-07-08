package com.msc.springai.controller;

import com.msc.springai.dto.learning.request.SaveDraftRequest;
import com.msc.springai.dto.learning.request.SummaryGenerateRequest;
import com.msc.springai.dto.learning.response.SavedSummaryResponse;
import com.msc.springai.dto.learning.response.SummaryGenerateResponse;
import com.msc.springai.dto.learning.response.SummarySaveResponse;
import com.msc.springai.security.CurrentUserUtil;
import com.msc.springai.service.SummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class SummaryController {

    private final SummaryService summaryService;

    @PostMapping("/api/courses/{courseId}/summary/generate")
    public SummaryGenerateResponse generateCourseSummary(
            @PathVariable Long courseId,
            @RequestBody(required = false) SummaryGenerateRequest request
    ) {
        Long currentUserId = CurrentUserUtil.getCurrentUserId();

        return summaryService.generateCourseSummary(
                currentUserId,
                courseId,
                request
        );
    }

    @PostMapping("/api/documents/{documentId}/summary/generate")
    public SummaryGenerateResponse generateDocumentSummary(
            @PathVariable Long documentId,
            @RequestBody(required = false) SummaryGenerateRequest request
    ) {
        Long currentUserId = CurrentUserUtil.getCurrentUserId();

        return summaryService.generateDocumentSummary(
                currentUserId,
                documentId,
                request
        );
    }

    @PostMapping("/api/summary/save")
    public SummarySaveResponse saveSummary(
            @RequestBody SaveDraftRequest request
    ) {
        Long currentUserId = CurrentUserUtil.getCurrentUserId();

        return summaryService.saveSummary(
                currentUserId,
                request
        );
    }

    @GetMapping("/api/courses/{courseId}/summaries")
    public List<SavedSummaryResponse> getCourseSummaries(
            @PathVariable Long courseId
    ) {
        Long currentUserId = CurrentUserUtil.getCurrentUserId();

        return summaryService.getCourseSummaries(
                currentUserId,
                courseId
        );
    }

    @DeleteMapping("/api/summary/{summaryId}")
    public Map<String, Object> deleteSummary(
            @PathVariable Long summaryId
    ) {
        Long currentUserId = CurrentUserUtil.getCurrentUserId();

        summaryService.deleteSummary(
                currentUserId,
                summaryId
        );

        return Map.of(
                "deleted", true,
                "summaryId", summaryId
        );
    }
}