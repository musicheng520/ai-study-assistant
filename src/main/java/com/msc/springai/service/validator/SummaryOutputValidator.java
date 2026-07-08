package com.msc.springai.service.validator;

import com.msc.springai.dto.learning.result.SummaryDefinitionResult;
import com.msc.springai.dto.learning.result.SummaryKeyConceptResult;
import com.msc.springai.dto.learning.result.SummaryResult;
import com.msc.springai.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
public class SummaryOutputValidator {

    private static final int MIN_SUMMARY_LENGTH = 50;

    private static final Set<String> VALID_SOURCE_SCOPES = Set.of(
            "COURSE",
            "DOCUMENT"
    );

    public void validate(SummaryResult result) {
        if (result == null) {
            throwInvalid();
        }

        if (isBlank(result.getTitle())) {
            throwInvalid();
        }

        if (isBlank(result.getSummary())) {
            throwInvalid();
        }

        if (result.getSummary().trim().length() < MIN_SUMMARY_LENGTH) {
            throwInvalid();
        }

        if (isBlank(result.getRevisionNotes())) {
            throwInvalid();
        }

        if (!VALID_SOURCE_SCOPES.contains(result.getSourceScope())) {
            throwInvalid();
        }

        validateKeyConcepts(result.getKeyConcepts());
        validateDefinitions(result.getDefinitions());
    }

    private void validateKeyConcepts(List<SummaryKeyConceptResult> keyConcepts) {
        if (keyConcepts == null || keyConcepts.isEmpty()) {
            throwInvalid();
        }

        for (SummaryKeyConceptResult concept : keyConcepts) {
            if (concept == null) {
                throwInvalid();
            }

            if (isBlank(concept.getName())) {
                throwInvalid();
            }

            if (isBlank(concept.getExplanation())) {
                throwInvalid();
            }
        }
    }

    private void validateDefinitions(List<SummaryDefinitionResult> definitions) {
        if (definitions == null || definitions.isEmpty()) {
            throwInvalid();
        }

        for (SummaryDefinitionResult definition : definitions) {
            if (definition == null) {
                throwInvalid();
            }

            if (isBlank(definition.getTerm())) {
                throwInvalid();
            }

            if (isBlank(definition.getDefinition())) {
                throwInvalid();
            }
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private void throwInvalid() {
        throw new BusinessException(
                "AI_OUTPUT_INVALID",
                "Generated summary is invalid. Please regenerate."
        );
    }
}