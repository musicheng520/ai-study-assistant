package com.msc.springai.service.validator;

import com.msc.springai.dto.learning.result.QuizQuestionResult;
import com.msc.springai.dto.learning.result.QuizResult;
import com.msc.springai.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
public class QuizOutputValidator {

    private static final String QUESTION_TYPE_MCQ = "MCQ";
    private static final String QUESTION_TYPE_SHORT_ANSWER = "SHORT_ANSWER";

    private static final Set<String> VALID_QUESTION_TYPES = Set.of(
            QUESTION_TYPE_MCQ,
            QUESTION_TYPE_SHORT_ANSWER
    );

    private static final Set<String> VALID_DIFFICULTIES = Set.of(
            "EASY",
            "MEDIUM",
            "HARD"
    );

    public void validate(
            QuizResult result,
            Integer expectedMcqCount,
            Integer expectedShortAnswerCount
    ) {
        if (result == null) {
            throwInvalid();
        }

        if (isBlank(result.getTitle())) {
            throwInvalid();
        }

        if (!VALID_DIFFICULTIES.contains(result.getDifficulty())) {
            throwInvalid();
        }

        int mcqCount = normalizeCount(expectedMcqCount);
        int shortAnswerCount = normalizeCount(expectedShortAnswerCount);
        int expectedTotal = mcqCount + shortAnswerCount;

        List<QuizQuestionResult> questions = result.getQuestions();

        if (questions == null || questions.size() != expectedTotal) {
            throwInvalid();
        }

        int actualMcqCount = 0;
        int actualShortAnswerCount = 0;

        for (QuizQuestionResult question : questions) {
            validateQuestion(question);

            if (QUESTION_TYPE_MCQ.equals(question.getQuestionType())) {
                actualMcqCount++;
            }

            if (QUESTION_TYPE_SHORT_ANSWER.equals(question.getQuestionType())) {
                actualShortAnswerCount++;
            }
        }

        if (actualMcqCount != mcqCount) {
            throwInvalid();
        }

        if (actualShortAnswerCount != shortAnswerCount) {
            throwInvalid();
        }
    }

    private void validateQuestion(QuizQuestionResult question) {
        if (question == null) {
            throwInvalid();
        }

        if (!VALID_QUESTION_TYPES.contains(question.getQuestionType())) {
            throwInvalid();
        }

        if (isBlank(question.getQuestionText())) {
            throwInvalid();
        }

        if (isBlank(question.getCorrectAnswer())) {
            throwInvalid();
        }

        if (isBlank(question.getExplanation())) {
            throwInvalid();
        }

        if (!VALID_DIFFICULTIES.contains(question.getDifficulty())) {
            throwInvalid();
        }

        if (isBlank(question.getTopic())) {
            throwInvalid();
        }

        if (QUESTION_TYPE_MCQ.equals(question.getQuestionType())) {
            validateMcqQuestion(question);
        }
    }

    private void validateMcqQuestion(QuizQuestionResult question) {
        List<String> options = question.getOptions();

        if (options == null || options.size() < 4) {
            throwInvalid();
        }

        for (String option : options) {
            if (isBlank(option)) {
                throwInvalid();
            }
        }

        if (!isCorrectAnswerInOptions(question.getCorrectAnswer(), options)) {
            throwInvalid();
        }
    }

    private boolean isCorrectAnswerInOptions(
            String correctAnswer,
            List<String> options
    ) {
        if (isBlank(correctAnswer) || options == null || options.isEmpty()) {
            return false;
        }

        String normalizedAnswer = normalize(correctAnswer);

        for (String option : options) {
            String normalizedOption = normalize(option);

            if (normalizedOption.equals(normalizedAnswer)) {
                return true;
            }

            if (normalizedOption.startsWith(normalizedAnswer + ".")) {
                return true;
            }

            if (normalizedOption.startsWith(normalizedAnswer + ")")) {
                return true;
            }
        }

        return false;
    }

    private int normalizeCount(Integer count) {
        return count == null ? 0 : count;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }

        return value
                .trim()
                .toUpperCase();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private void throwInvalid() {
        throw new BusinessException(
                "AI_OUTPUT_INVALID",
                "Generated quiz is invalid. Please regenerate."
        );
    }
}