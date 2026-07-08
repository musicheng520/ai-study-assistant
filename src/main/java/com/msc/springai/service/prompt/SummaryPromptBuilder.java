package com.msc.springai.service.prompt;

import com.msc.springai.dto.learning.retrieval.RetrievedChunk;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SummaryPromptBuilder {

    private static final int MAX_CHUNK_CONTENT_LENGTH = 400;

    public String buildSummaryPrompt(
            String sourceScope,
            List<RetrievedChunk> chunks
    ) {
        String context = buildContext(chunks);

        return """
        You are an AI study assistant.

        Use only the context below.

        Return complete valid JSON only. Do not use markdown.

        Required JSON:
        {
          "title": "short title",
          "summary": "50 to 80 words",
          "keyConcepts": [
            {
              "name": "short concept name",
              "explanation": "one short sentence"
            }
          ],
          "definitions": [
            {
              "term": "short term",
              "definition": "one short sentence"
            }
          ],
          "revisionNotes": "one short paragraph",
          "sourceScope": "%s"
        }

        Rules:
        - sourceScope must be exactly "%s".
        - Generate exactly 2 keyConcepts.
        - Generate exactly 2 definitions.
        - Keep every string short.
        - Make sure the JSON object is complete and all arrays are closed.

        Context:
        %s
        """.formatted(
                sourceScope,
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