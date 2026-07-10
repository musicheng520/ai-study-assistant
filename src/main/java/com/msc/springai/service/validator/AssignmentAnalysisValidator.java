package com.msc.springai.service.validator;

import com.msc.springai.dto.workflow.assignment.AssignmentAnalysisResult;
import com.msc.springai.exception.BusinessException;
import org.springframework.stereotype.Component;

@Component
public class AssignmentAnalysisValidator {

    public void validate(AssignmentAnalysisResult result) {
        if (result == null) {
            throw new BusinessException(
                    "INVALID_ASSIGNMENT_ANALYSIS",
                    "Assignment analysis result is empty."
            );
        }

        if (result.getRequirements() == null || result.getRequirements().isEmpty()) {
            throw new BusinessException(
                    "INVALID_ASSIGNMENT_ANALYSIS",
                    "Assignment requirements cannot be empty."
            );
        }

        if (result.getDeliverables() == null || result.getDeliverables().isEmpty()) {
            throw new BusinessException(
                    "INVALID_ASSIGNMENT_ANALYSIS",
                    "Assignment deliverables cannot be empty."
            );
        }

        if (result.getChecklist() == null || result.getChecklist().isEmpty()) {
            throw new BusinessException(
                    "INVALID_ASSIGNMENT_ANALYSIS",
                    "Assignment checklist cannot be empty."
            );
        }

        if (result.getHighScoreTips() == null || result.getHighScoreTips().isBlank()) {
            throw new BusinessException(
                    "INVALID_ASSIGNMENT_ANALYSIS",
                    "High score tips cannot be empty."
            );
        }

        if (result.getSuggestedStructure() == null) {
            result.setSuggestedStructure(java.util.List.of());
        }

        if (result.getRiskWarnings() == null) {
            result.setRiskWarnings(java.util.List.of());
        }
    }
}