package com.msc.springai.service;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmbeddingService {

    private final EmbeddingModel embeddingModel;

    public float[] embed(String text) {
        System.out.println("[EmbeddingService] Start embedding text.");

        if (text == null || text.isBlank()) {
            throw new RuntimeException("Text for embedding is empty");
        }

        System.out.println("[EmbeddingService] Text length: " + text.length());

        int previewLength = Math.min(200, text.length());
        System.out.println("[EmbeddingService] Text preview: "
                + text.substring(0, previewLength));

        try {
            float[] embedding = embeddingModel.embed(text);

            if (embedding == null || embedding.length == 0) {
                throw new RuntimeException("Embedding result is empty");
            }

            System.out.println("[EmbeddingService] Embedding generated successfully.");
            System.out.println("[EmbeddingService] Embedding dimension: " + embedding.length);

            System.out.println("[EmbeddingService] First values preview: "
                    + previewEmbedding(embedding, 8));

            return embedding;

        } catch (Exception e) {
            System.out.println("[EmbeddingService] Embedding failed: " + e.getMessage());
            throw new RuntimeException("Failed to generate embedding: " + e.getMessage(), e);
        }
    }

    public float[][] embedBatch(java.util.List<String> texts) {
        System.out.println("[EmbeddingService] Start batch embedding.");
        System.out.println("[EmbeddingService] Text count: "
                + (texts == null ? 0 : texts.size()));

        if (texts == null || texts.isEmpty()) {
            throw new RuntimeException("Texts for embedding are empty");
        }

        float[][] results = new float[texts.size()][];

        for (int i = 0; i < texts.size(); i++) {
            System.out.println("[EmbeddingService] Embedding item index: " + i);
            results[i] = embed(texts.get(i));
        }

        System.out.println("[EmbeddingService] Batch embedding finished.");

        return results;
    }

    private String previewEmbedding(float[] embedding, int limit) {
        StringBuilder builder = new StringBuilder("[");

        int size = Math.min(limit, embedding.length);

        for (int i = 0; i < size; i++) {
            builder.append(embedding[i]);

            if (i < size - 1) {
                builder.append(", ");
            }
        }

        if (embedding.length > limit) {
            builder.append(", ...");
        }

        builder.append("]");

        return builder.toString();
    }
}