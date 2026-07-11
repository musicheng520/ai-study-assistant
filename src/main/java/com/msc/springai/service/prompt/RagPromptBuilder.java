package com.msc.springai.service.prompt;

import com.msc.springai.dto.document.RedisChunkSearchResult;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RagPromptBuilder {

    private static final int MAX_CONTEXT_CHARS = 12000;
    private static final int MAX_SINGLE_CHUNK_CHARS = 1800;

    public String buildCourseRagPrompt(
            String question,
            List<RedisChunkSearchResult> chunks
    ) {
        System.out.println("[RagPromptBuilder] Start building course RAG prompt.");

        if (question == null || question.isBlank()) {
            throw new RuntimeException("Question is required");
        }

        if (chunks == null || chunks.isEmpty()) {
            throw new RuntimeException("Chunks are required to build RAG prompt");
        }

        System.out.println("[RagPromptBuilder] Question = " + question);
        System.out.println("[RagPromptBuilder] Retrieved chunk count = " + chunks.size());

        StringBuilder contextBuilder = new StringBuilder();

        int citationIndex = 1;

        for (RedisChunkSearchResult chunk : chunks) {
            if (chunk == null || chunk.getContent() == null || chunk.getContent().isBlank()) {
                continue;
            }

            String content = limitText(chunk.getContent(), MAX_SINGLE_CHUNK_CHARS);

            String sourceBlock = buildSourceBlock(citationIndex, chunk, content);

            if (contextBuilder.length() + sourceBlock.length() > MAX_CONTEXT_CHARS) {
                System.out.println("[RagPromptBuilder] Context limit reached. Stop adding chunks.");
                break;
            }

            contextBuilder.append(sourceBlock).append("\n\n");

            System.out.println("[RagPromptBuilder] Added source [" + citationIndex + "]"
                    + ", chunkId = " + chunk.getChunkId()
                    + ", documentId = " + chunk.getDocumentId()
                    + ", pageNumber = " + chunk.getPageNumber());

            citationIndex++;
        }

        String context = contextBuilder.toString().trim();

        if (context.isBlank()) {
            throw new RuntimeException("No usable context found from retrieved chunks");
        }

        String prompt = """
                You are an AI study assistant for international students.

                Your task:
                Answer the user's question using the provided course document sources as the primary ground truth.
           
                Rules:
                1. Use simple and clear English.
                2. Do not use outside knowledge unless it is only for basic explanation.
                3. If the sources do not contain enough information, say:
                   "I do not have enough information in the uploaded course documents to answer this."
                4. When you use information from a source, cite it using [1], [2], [3] format.
                5. Do not invent citations.
                6. Do not mention Redis, embeddings, vector database, or backend implementation.
                7.You may explain concepts in simple student-friendly language, but you must not add new course-specific facts that are not supported by the sources.        
                8. If useful, explain step by step.

                Sources:
                %s

                User question:
                %s

                Answer:
                """.formatted(context, question);

        System.out.println("[RagPromptBuilder] Prompt built successfully.");
        System.out.println("[RagPromptBuilder] Prompt length = " + prompt.length());

        return prompt;
    }

    private String buildSourceBlock(
            int citationIndex,
            RedisChunkSearchResult chunk,
            String content
    ) {
        return """
                [Source %d]
                citation: [%d]
                documentId: %s
                chunkId: %s
                fileName: %s
                pageNumber: %s
                sectionTitle: %s
                distance: %s
                content:
                %s
                """.formatted(
                citationIndex,
                citationIndex,
                safe(chunk.getDocumentId()),
                safe(chunk.getChunkId()),
                safe(chunk.getFileName()),
                safe(chunk.getPageNumber()),
                safe(chunk.getSectionTitle()),
                safe(chunk.getDistance()),
                content
        );
    }

    private String limitText(String text, int maxChars) {
        if (text == null) {
            return "";
        }

        if (text.length() <= maxChars) {
            return text;
        }

        return text.substring(0, maxChars) + "...";
    }

    private String safe(Object value) {
        if (value == null) {
            return "N/A";
        }

        return String.valueOf(value);
    }
}