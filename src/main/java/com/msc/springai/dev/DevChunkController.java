package com.msc.springai.dev;


import com.msc.springai.dto.document.ExtractedDocument;
import com.msc.springai.entity.DocumentChunk;
import com.msc.springai.service.ChunkingService;
import com.msc.springai.service.DocumentTextExtractor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Profile("dev")
@RestController
@RequiredArgsConstructor
public class DevChunkController {

    private final DocumentTextExtractor documentTextExtractor;
    private final ChunkingService chunkingService;

    @GetMapping("/api/dev/chunk-document")
    public Map<String, Object> chunkDocument(
            @RequestParam String path,
            @RequestParam String fileType,
            @RequestParam Long userId,
            @RequestParam Long courseId,
            @RequestParam Long documentId
    ) {
        System.out.println("[DevChunkController] Start chunk document test.");
        System.out.println("[DevChunkController] path = " + path);
        System.out.println("[DevChunkController] fileType = " + fileType);
        System.out.println("[DevChunkController] userId = " + userId);
        System.out.println("[DevChunkController] courseId = " + courseId);
        System.out.println("[DevChunkController] documentId = " + documentId);

        ExtractedDocument extractedDocument = documentTextExtractor.extract(
                Paths.get(path),
                fileType
        );

        System.out.println("[DevChunkController] Extracted text length = "
                + extractedDocument.getFullText().length());

        List<DocumentChunk> chunks = chunkingService.buildChunks(
                userId,
                courseId,
                documentId,
                extractedDocument
        );

        Map<String, Object> response = new HashMap<>();

        response.put("totalPages", extractedDocument.getTotalPages());
        response.put("pageCount", extractedDocument.getPages().size());
        response.put("chunkCount", chunks.size());

        if (!chunks.isEmpty()) {
            DocumentChunk firstChunk = chunks.get(0);

            response.put("firstChunkIndex", firstChunk.getChunkIndex());
            response.put("firstChunkPageNumber", firstChunk.getPageNumber());
            response.put("firstChunkLength", firstChunk.getContent().length());
            response.put("firstChunkTokenCount", firstChunk.getTokenCount());
            response.put("firstChunkHash", firstChunk.getContentHash());
            response.put(
                    "firstChunkPreview",
                    firstChunk.getContent().substring(
                            0,
                            Math.min(500, firstChunk.getContent().length())
                    )
            );
        }

        System.out.println("[DevChunkController] Chunk document test finished.");
        System.out.println("[DevChunkController] chunkCount = " + chunks.size());

        return response;
    }
}