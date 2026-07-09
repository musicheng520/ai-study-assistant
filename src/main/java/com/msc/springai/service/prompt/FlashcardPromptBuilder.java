package com.msc.springai.service.prompt;

import com.msc.springai.dto.learning.retrieval.RetrievedChunk;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class FlashcardPromptBuilder {

    private static final int MAX_CHUNK_CONTENT_LENGTH = 500;

    public String buildFlashcardPrompt(
            String sourceScope,
            int count,
            String difficulty,
            List<RetrievedChunk> chunks
    ) {
        String context = buildContext(chunks);

        return """
                You are an AI study assistant.

                Use only the context below to generate flashcards.

                Return complete valid JSON only. Do not use markdown.

                Required JSON:
                {
                  "title": "short flashcard set title",
                  "cards": [
                    {
                      "front": "short question or term",
                      "back": "short answer or explanation",
                      "topic": "short topic",
                      "difficulty": "%s",
                      "sourceChunkId": 1
                    }
                  ]
                }

                Rules:
                - Generate exactly %d flashcards.
                - difficulty must be exactly "%s".
                - front and back must not be the same.
                - Keep every string short.
                - Make sure the JSON object is complete and all arrays are closed.

                sourceScope: %s

                Context:
                %s
                """.formatted(
                difficulty,
                count,
                difficulty,
                sourceScope,
                context
        );
    }

    public String buildWeakTopicFlashcardPrompt(
            List<String> topics,
            int cardsPerTopic,
            String difficulty,
            List<RetrievedChunk> chunks
    ) {
        String context = buildContext(chunks);
        int totalCards = topics.size() * cardsPerTopic;

        return """
            You are an AI study assistant.

            The student has weak topics based on quiz wrong answers.

            Use only the context below to generate targeted flashcards.

            Return complete valid JSON only. Do not use markdown.

            Required JSON:
            {
              "title": "Weak Topic Flashcards",
              "cards": [
                {
                  "front": "short question or term",
                  "back": "short answer or explanation",
                  "topic": "one of the weak topics",
                  "difficulty": "%s",
                  "sourceChunkId": 1
                }
              ]
            }

            Rules:
            - Generate exactly %d flashcards in total.
            - Generate about %d flashcards for each weak topic.
            - Every card topic must be one of these weak topics: %s
            - difficulty must be exactly "%s".
            - front and back must not be the same.
            - Keep every card focused on exam revision.
            - Do not invent content outside the provided context.
            - Make sure the JSON object is complete and all arrays are closed.

            Weak topics:
            %s

            Context:
            %s
            """.formatted(
                difficulty,
                totalCards,
                cardsPerTopic,
                topics,
                difficulty,
                String.join(", ", topics),
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