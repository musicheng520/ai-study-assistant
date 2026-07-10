package com.msc.springai.controller;

import com.msc.springai.dto.workflow.rubric.RubricAnalysisResponse;
import com.msc.springai.dto.workflow.rubric.RubricAnalyzeRequest;
import com.msc.springai.security.CurrentUserUtil;
import com.msc.springai.service.RubricAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RubricAnalysisController {

    private final RubricAnalysisService rubricAnalysisService;

    @PostMapping("/documents/{documentId}/rubric/analyze")
    public RubricAnalysisResponse analyzeRubric(
            @PathVariable Long documentId,
            @RequestBody(required = false) RubricAnalyzeRequest request
    ) {
        Long currentUserId = CurrentUserUtil.getCurrentUserId();

        return rubricAnalysisService.analyzeRubric(
                currentUserId,
                documentId,
                request
        );
    }

    @GetMapping("/courses/{courseId}/rubric-analyses")
    public List<RubricAnalysisResponse> getCourseRubricAnalyses(
            @PathVariable Long courseId
    ) {
        Long currentUserId = CurrentUserUtil.getCurrentUserId();

        return rubricAnalysisService.getCourseRubricAnalyses(
                currentUserId,
                courseId
        );
    }
}