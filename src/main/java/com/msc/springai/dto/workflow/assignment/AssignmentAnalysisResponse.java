package com.msc.springai.dto.workflow.assignment;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AssignmentAnalysisResponse {

    private Long id;

    private Long courseId;

    private Long documentId;

    private List<String> requirements;

    private List<String> deliverables;

    private LocalDateTime deadline;

    private List<String> checklist;

    private String highScoreTips;

    private List<String> suggestedStructure;

    private List<String> riskWarnings;

    private LocalDateTime createdAt;
}