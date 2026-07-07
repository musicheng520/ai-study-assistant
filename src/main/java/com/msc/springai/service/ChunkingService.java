package com.msc.springai.service;

import com.msc.springai.dto.document.ExtractedDocument;
import com.msc.springai.dto.document.ExtractedPage;
import com.msc.springai.entity.DocumentChunk;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChunkingService {

    private static final int CHUNK_SIZE = 1200;
    private static final int CHUNK_OVERLAP = 180;

    private final TextCleaningService textCleaningService;

    public List<DocumentChunk> buildChunks(
            Long userId,
            Long courseId,
            Long documentId,
            ExtractedDocument extractedDocument
    ) {
        System.out.println("[ChunkingService] Start building chunks.");
        System.out.println("[ChunkingService] userId = " + userId);
        System.out.println("[ChunkingService] courseId = " + courseId);
        System.out.println("[ChunkingService] documentId = " + documentId);

        if (extractedDocument == null) {
            throw new RuntimeException("Extracted document is required");
        }

        if (extractedDocument.getPages() == null || extractedDocument.getPages().isEmpty()) {
            throw new RuntimeException("Extracted document pages are empty");
        }

        System.out.println("[ChunkingService] Extracted total pages: "
                + extractedDocument.getTotalPages());
        System.out.println("[ChunkingService] Extracted page list size: "
                + extractedDocument.getPages().size());

        List<DocumentChunk> chunks = new ArrayList<>();

        int chunkIndex = 0;

        for (ExtractedPage page : extractedDocument.getPages()) {
            Integer pageNumber = page.getPageNumber();

            System.out.println("[ChunkingService] Processing page: " + pageNumber);

            String cleanedPageText = textCleaningService.clean(page.getText());

            if (!textCleaningService.isUsable(cleanedPageText)) {
                System.out.println("[ChunkingService] Skip unusable page: " + pageNumber);
                continue;
            }

            List<String> pageChunks = splitText(cleanedPageText);

            System.out.println("[ChunkingService] Page " + pageNumber
                    + " generated chunk count: " + pageChunks.size());

            for (String chunkText : pageChunks) {
                String cleanedChunk = textCleaningService.clean(chunkText);

                if (!textCleaningService.isUsable(cleanedChunk)) {
                    System.out.println("[ChunkingService] Skip unusable chunk on page: "
                            + pageNumber);
                    continue;
                }

                DocumentChunk chunk = new DocumentChunk();

                chunk.setUserId(userId);
                chunk.setCourseId(courseId);
                chunk.setDocumentId(documentId);
                chunk.setChunkIndex(chunkIndex);
                chunk.setContent(cleanedChunk);
                chunk.setContentHash(sha256(cleanedChunk));
                chunk.setPageNumber(pageNumber);
                chunk.setSectionTitle(null);
                chunk.setTokenCount(estimateTokenCount(cleanedChunk));
                chunk.setVectorKey(null);
                chunk.setVectorStatus("PENDING");
                chunk.setEmbeddingModel(null);
                chunk.setEmbeddingDimension(null);

                chunks.add(chunk);

                System.out.println("[ChunkingService] Created chunk index: " + chunkIndex
                        + ", page: " + pageNumber
                        + ", length: " + cleanedChunk.length()
                        + ", estimated tokens: " + chunk.getTokenCount());

                chunkIndex++;
            }
        }

        if (chunks.isEmpty()) {
            throw new RuntimeException("No usable chunks generated from this document");
        }

        System.out.println("[ChunkingService] Finished building chunks.");
        System.out.println("[ChunkingService] Total chunks: " + chunks.size());

        return chunks;
    }

    private List<String> splitText(String text) {
        System.out.println("[ChunkingService] Start splitting text.");
        System.out.println("[ChunkingService] Chunk size: " + CHUNK_SIZE);
        System.out.println("[ChunkingService] Chunk overlap: " + CHUNK_OVERLAP);

        List<String> chunks = new ArrayList<>();

        if (text == null || text.isBlank()) {
            System.out.println("[ChunkingService] Text is blank, return empty chunks.");
            return chunks;
        }

        int start = 0;
        int textLength = text.length();

        while (start < textLength) {
            int end = Math.min(start + CHUNK_SIZE, textLength);

            String chunk = text.substring(start, end).trim();

            if (!chunk.isBlank()) {
                chunks.add(chunk);
            }

            System.out.println("[ChunkingService] Split chunk: start = "
                    + start + ", end = " + end);

            if (end >= textLength) {
                break;
            }

            start = Math.max(0, end - CHUNK_OVERLAP);
        }

        System.out.println("[ChunkingService] Split result count: " + chunks.size());

        return chunks;
    }

    private int estimateTokenCount(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }

        // 粗略估算：英文里 1 token 大约 4 个字符
        return Math.max(1, text.length() / 4);
    }

    private String sha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate content hash");
        }
    }
}