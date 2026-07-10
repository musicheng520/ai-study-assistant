package com.msc.springai.dto.workflow.rubric;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RubricAnalysisResponse {

    private Long id;

    private Long courseId;

    private Long documentId;

    private List<RubricCriterionResult> criteria;

    private List<String> excellentBand;

    private String commonMistakes;

    private String highScoreStrategy;

    private LocalDateTime createdAt;
}