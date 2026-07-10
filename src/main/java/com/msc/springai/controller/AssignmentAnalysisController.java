package com.msc.springai.controller;

import com.msc.springai.dto.workflow.assignment.AssignmentAnalysisResponse;
import com.msc.springai.dto.workflow.assignment.AssignmentAnalyzeRequest;
import com.msc.springai.security.CurrentUserUtil;
import com.msc.springai.service.AssignmentAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AssignmentAnalysisController {

    private final AssignmentAnalysisService assignmentAnalysisService;

    @PostMapping("/documents/{documentId}/assignment/analyze")
    public AssignmentAnalysisResponse analyzeAssignment(
            @PathVariable Long documentId,
            @RequestBody(required = false) AssignmentAnalyzeRequest request
    ) {
        Long currentUserId = CurrentUserUtil.getCurrentUserId();

        return assignmentAnalysisService.analyzeAssignment(
                currentUserId,
                documentId,
                request
        );
    }

    @GetMapping("/courses/{courseId}/assignment-analyses")
    public List<AssignmentAnalysisResponse> getCourseAssignmentAnalyses(
            @PathVariable Long courseId
    ) {
        Long currentUserId = CurrentUserUtil.getCurrentUserId();

        return assignmentAnalysisService.getCourseAssignmentAnalyses(
                currentUserId,
                courseId
        );
    }
}