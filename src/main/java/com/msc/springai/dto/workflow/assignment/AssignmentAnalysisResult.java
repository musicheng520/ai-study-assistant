package com.msc.springai.dto.workflow.assignment;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AssignmentAnalysisResult {

    private List<String> requirements;

    private List<String> deliverables;

    private String deadline;

    private List<String> checklist;

    private String highScoreTips;

    private List<String> suggestedStructure;

    private List<String> riskWarnings;
}