package com.msc.springai.service.validator;

import com.msc.springai.dto.learning.result.FlashcardItemResult;
import com.msc.springai.dto.learning.result.FlashcardResult;
import com.msc.springai.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
public class FlashcardOutputValidator {

    private static final Set<String> VALID_DIFFICULTIES = Set.of(
            "EASY",
            "MEDIUM",
            "HARD"
    );

    public void validate(
            FlashcardResult result,
            Integer expectedCount
    ) {
        if (result == null) {
            throwInvalid();
        }

        if (isBlank(result.getTitle())) {
            throwInvalid();
        }

        int count = expectedCount == null ? 0 : expectedCount;

        List<FlashcardItemResult> cards = result.getCards();

        if (cards == null || cards.size() != count) {
            throwInvalid();
        }

        for (FlashcardItemResult card : cards) {
            validateCard(card);
        }
    }

    private void validateCard(FlashcardItemResult card) {
        if (card == null) {
            throwInvalid();
        }

        if (isBlank(card.getFront())) {
            throwInvalid();
        }

        if (isBlank(card.getBack())) {
            throwInvalid();
        }

        if (isBlank(card.getTopic())) {
            throwInvalid();
        }

        if (!VALID_DIFFICULTIES.contains(card.getDifficulty())) {
            throwInvalid();
        }

        if (card.getFront().trim().equalsIgnoreCase(card.getBack().trim())) {
            throwInvalid();
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private void throwInvalid() {
        throw new BusinessException(
                "AI_OUTPUT_INVALID",
                "Generated flashcards are invalid. Please regenerate."
        );
    }
}