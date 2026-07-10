package com.msc.springai.dto.workflow.rubric;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RubricAnalysisResult {

    private List<RubricCriterionResult> criteria;

    private List<String> excellentBand;

    private String commonMistakes;

    private String highScoreStrategy;
}