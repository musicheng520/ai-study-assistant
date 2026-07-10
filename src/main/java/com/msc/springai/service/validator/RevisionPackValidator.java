package com.msc.springai.service.validator;

import com.msc.springai.dto.workflow.revision.RevisionPackResult;
import com.msc.springai.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RevisionPackValidator {

    public void validate(RevisionPackResult result) {
        if (result == null) {
            throw new BusinessException(
                    "INVALID_REVISION_PACK",
                    "Revision pack result is empty."
            );
        }

        if (result.getTitle() == null || result.getTitle().isBlank()) {
            throw new BusinessException(
                    "INVALID_REVISION_PACK",
                    "Revision pack title cannot be empty."
            );
        }

        if (result.getSummary() == null || result.getSummary().isBlank()) {
            throw new BusinessException(
                    "INVALID_REVISION_PACK",
                    "Revision pack summary cannot be empty."
            );
        }

        if (result.getWeakTopics() == null) {
            result.setWeakTopics(List.of());
        }

        if (result.getReviewOrder() == null) {
            result.setReviewOrder(List.of());
        }

        if (result.getRecommendedActions() == null || result.getRecommendedActions().isEmpty()) {
            throw new BusinessException(
                    "INVALID_REVISION_PACK",
                    "Recommended actions cannot be empty."
            );
        }

        if (result.getRelatedDocuments() == null) {
            result.setRelatedDocuments(List.of());
        }

        if (result.getStudyTasks() == null) {
            result.setStudyTasks(List.of());
        }

        if (result.getSuggestedFlashcards() == null) {
            result.setSuggestedFlashcards(List.of());
        }
    }
}