package com.msc.springai.service.grading;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ShortAnswerGradingService {

    private static final double PASSING_SCORE = 0.65;

    private static final Set<String> STOP_WORDS = Set.of(
            "the", "a", "an", "and", "or", "to", "of", "in", "on", "for",
            "with", "is", "are", "was", "were", "be", "been", "being",
            "that", "this", "it", "as", "by", "from", "at", "which",
            "what", "when", "where", "how", "why", "can", "could",
            "should", "would", "may", "might", "will", "shall"
    );

    private final ChatClient.Builder chatClientBuilder;
    private final ObjectMapper objectMapper;

    public boolean grade(
            String questionText,
            String userAnswer,
            String correctAnswer,
            String explanation
    ) {
        if (isBlank(userAnswer)) {
            return false;
        }

        Boolean deterministicResult = tryDeterministicGrade(
                userAnswer,
                correctAnswer
        );

        if (deterministicResult != null) {
            return deterministicResult;
        }

        try {
            return gradeWithLlm(
                    questionText,
                    userAnswer,
                    correctAnswer,
                    explanation
            );
        } catch (Exception e) {
            System.out.println("[ShortAnswerGradingService] LLM grading failed.");
            System.out.println("[ShortAnswerGradingService] fallback to keyword grading.");
            System.out.println("[ShortAnswerGradingService] error = " + e.getMessage());

            return fallbackKeywordGrade(
                    userAnswer,
                    correctAnswer,
                    explanation
            );
        }
    }

    private Boolean tryDeterministicGrade(
            String userAnswer,
            String correctAnswer
    ) {
        if (isBlank(correctAnswer)) {
            return null;
        }

        String normalizedUserAnswer = normalizeText(userAnswer);
        String normalizedCorrectAnswer = normalizeText(correctAnswer);

        if (normalizedUserAnswer.isBlank()) {
            return false;
        }

        if (normalizedUserAnswer.equals(normalizedCorrectAnswer)) {
            return true;
        }

        /*
         * 如果学生答案完整包含标准答案，直接判对。
         * 例如：
         * correctAnswer = "automatic memory management"
         * userAnswer = "It means automatic memory management in Java."
         */
        if (normalizedCorrectAnswer.length() >= 12 &&
                normalizedUserAnswer.contains(normalizedCorrectAnswer)) {
            return true;
        }

        /*
         * 太短的答案一般不能算简答题正确。
         * 例如：
         * "yes"
         * "idk"
         * "A"
         */
        if (normalizedUserAnswer.length() < 8) {
            return false;
        }

        return null;
    }

    private boolean gradeWithLlm(
            String questionText,
            String userAnswer,
            String correctAnswer,
            String explanation
    ) throws Exception {
        String prompt = buildGradingPrompt(
                questionText,
                userAnswer,
                correctAnswer,
                explanation
        );

        String raw = chatClientBuilder
                .build()
                .prompt()
                .user(prompt)
                .call()
                .content();

        if (raw == null || raw.isBlank()) {
            throw new IllegalStateException("LLM returned empty grading result.");
        }

        String json = extractJson(raw);

        JsonNode node = objectMapper.readTree(json);

        boolean modelCorrect = node.path("correct").asBoolean(false);
        double score = node.path("score").asDouble(0.0);

        System.out.println("[ShortAnswerGradingService] modelCorrect = " + modelCorrect);
        System.out.println("[ShortAnswerGradingService] score = " + score);
        System.out.println("[ShortAnswerGradingService] reason = " + node.path("reason").asText(""));

        return modelCorrect && score >= PASSING_SCORE;
    }

    private String buildGradingPrompt(
            String questionText,
            String userAnswer,
            String correctAnswer,
            String explanation
    ) {
        return """
                You are grading a student's short-answer quiz response.

                Your task:
                Decide whether the student's answer is semantically correct.

                Return valid JSON only. Do not use markdown.

                Required JSON format:
                {
                  "correct": true,
                  "score": 0.0,
                  "reason": "short reason"
                }

                Grading rules:
                - Score must be between 0.0 and 1.0.
                - correct should be true only when the student's answer captures the main meaning.
                - Do not require exact wording.
                - Ignore minor grammar or spelling mistakes.
                - Mark false if the answer is empty, vague, unrelated, or contradicts the standard answer.
                - Mark false if the answer only repeats keywords without showing the correct meaning.
                - Use the explanation as additional reference.

                Question:
                %s

                Standard Answer:
                %s

                Explanation:
                %s

                Student Answer:
                %s
                """.formatted(
                safe(questionText),
                safe(correctAnswer),
                safe(explanation),
                safe(userAnswer)
        );
    }

    private boolean fallbackKeywordGrade(
            String userAnswer,
            String correctAnswer,
            String explanation
    ) {
        if (isBlank(userAnswer)) {
            return false;
        }

        String referenceText = safe(correctAnswer) + " " + safe(explanation);

        Set<String> keywords = extractKeywords(referenceText);

        if (keywords.isEmpty()) {
            return false;
        }

        String normalizedUserAnswer = normalizeText(userAnswer);

        long matchedCount = keywords.stream()
                .filter(normalizedUserAnswer::contains)
                .count();

        double ratio = matchedCount * 1.0 / keywords.size();

        return matchedCount >= 2 && ratio >= 0.45;
    }

    private Set<String> extractKeywords(String text) {
        String normalized = normalizeText(text);

        return Arrays.stream(normalized.split("\\s+"))
                .map(String::trim)
                .filter(word -> word.length() >= 4)
                .filter(word -> !STOP_WORDS.contains(word))
                .collect(Collectors.toSet());
    }

    private String extractJson(String raw) {
        String cleaned = raw.trim();

        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7).trim();
        }

        if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3).trim();
        }

        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3).trim();
        }

        int start = cleaned.indexOf("{");
        int end = cleaned.lastIndexOf("}");

        if (start < 0 || end < 0 || end <= start) {
            throw new IllegalStateException("LLM did not return valid JSON.");
        }

        return cleaned.substring(start, end + 1);
    }

    private String normalizeText(String text) {
        if (text == null) {
            return "";
        }

        return text
                .toLowerCase()
                .replaceAll("[^a-z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String safe(String value) {
        if (value == null) {
            return "";
        }

        String cleaned = value
                .replaceAll("\\s+", " ")
                .trim();

        if (cleaned.length() <= 1200) {
            return cleaned;
        }

        return cleaned.substring(0, 1200) + "...";
    }
}