package com.msc.springai.service.prompt;

import com.msc.springai.dto.learning.retrieval.RetrievedChunk;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class QuizPromptBuilder {

    private static final int MAX_CHUNK_CONTENT_LENGTH = 500;

    public String buildQuizPrompt(
            String sourceScope,
            String difficulty,
            int mcqCount,
            int shortAnswerCount,
            List<RetrievedChunk> chunks
    ) {
        String context = buildContext(chunks);
        int totalCount = mcqCount + shortAnswerCount;

        return """
                You are an AI study assistant.

                Use only the context below to generate a quiz.

                Return complete valid JSON only. Do not use markdown.

                Required JSON:
                {
                  "title": "short quiz title",
                  "difficulty": "%s",
                  "questions": [
                    {
                      "questionType": "MCQ",
                      "questionText": "question text",
                      "options": ["A. option", "B. option", "C. option", "D. option"],
                      "correctAnswer": "A. option",
                      "explanation": "one short explanation",
                      "difficulty": "%s",
                      "topic": "short topic",
                      "sourceChunkId": 1
                    },
                    {
                      "questionType": "SHORT_ANSWER",
                      "questionText": "question text",
                      "options": [],
                      "correctAnswer": "short answer",
                      "explanation": "one short explanation",
                      "difficulty": "%s",
                      "topic": "short topic",
                      "sourceChunkId": 1
                    }
                  ]
                }

                Rules:
                - Generate exactly %d questions in total.
                - Generate exactly %d MCQ questions.
                - Generate exactly %d SHORT_ANSWER questions.
                - difficulty must be exactly "%s".
                - For MCQ, options must have exactly 4 items.
                - For MCQ, correctAnswer must exactly match one option.
                - For SHORT_ANSWER, options must be [].
                - Keep every string short.
                - Make sure the JSON object is complete and all arrays are closed.

                sourceScope: %s

                Context:
                %s
                """.formatted(
                difficulty,
                difficulty,
                difficulty,
                totalCount,
                mcqCount,
                shortAnswerCount,
                difficulty,
                sourceScope,
                context
        );
    }

    private String buildContext(List<RetrievedChunk> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return "";
        }

        StringBuilder builder = new StringBuilder();

        for (RetrievedChunk chunk : chunks) {
            builder.append("\n--- CHUNK ")
                    .append(chunk.getChunkId())
                    .append(" ---\n");

            builder.append("fileName: ")
                    .append(chunk.getFileName())
                    .append("\n");

            builder.append("pageNumber: ")
                    .append(chunk.getPageNumber())
                    .append("\n");

            builder.append("content:\n")
                    .append(truncate(chunk.getContent()))
                    .append("\n");
        }

        return builder.toString();
    }

    private String truncate(String content) {
        if (content == null) {
            return "";
        }

        String cleaned = content
                .replaceAll("\\s+", " ")
                .trim();

        if (cleaned.length() <= MAX_CHUNK_CONTENT_LENGTH) {
            return cleaned;
        }

        return cleaned.substring(0, MAX_CHUNK_CONTENT_LENGTH) + "...";
    }
}