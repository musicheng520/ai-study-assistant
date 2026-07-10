package com.msc.springai.service.validator;

import com.msc.springai.dto.workflow.rubric.RubricAnalysisResult;
import com.msc.springai.exception.BusinessException;
import org.springframework.stereotype.Component;

@Component
public class RubricAnalysisValidator {

    public void validate(RubricAnalysisResult result) {
        if (result == null) {
            throw new BusinessException(
                    "INVALID_RUBRIC_ANALYSIS",
                    "Rubric analysis result is empty."
            );
        }

        if (result.getCriteria() == null || result.getCriteria().isEmpty()) {
            throw new BusinessException(
                    "INVALID_RUBRIC_ANALYSIS",
                    "Rubric criteria cannot be empty."
            );
        }

        if (result.getExcellentBand() == null || result.getExcellentBand().isEmpty()) {
            throw new BusinessException(
                    "INVALID_RUBRIC_ANALYSIS",
                    "Excellent band cannot be empty."
            );
        }

        if (result.getCommonMistakes() == null || result.getCommonMistakes().isBlank()) {
            throw new BusinessException(
                    "INVALID_RUBRIC_ANALYSIS",
                    "Common mistakes cannot be empty."
            );
        }

        if (result.getHighScoreStrategy() == null || result.getHighScoreStrategy().isBlank()) {
            throw new BusinessException(
                    "INVALID_RUBRIC_ANALYSIS",
                    "High score strategy cannot be empty."
            );
        }
    }
}