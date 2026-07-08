package com.msc.springai.dev;

import com.msc.springai.dto.document.RedisChunkSearchResult;
import com.msc.springai.entity.DocumentChunk;
import com.msc.springai.service.EmbeddingService;
import com.msc.springai.service.RedisVectorService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;



import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

@Profile("dev")
@RestController
@RequiredArgsConstructor
public class DevRedisVectorController {

    private final EmbeddingService embeddingService;
    private final RedisVectorService redisVectorService;

    @GetMapping("/api/dev/vector-save-text")
    public Map<String, Object> saveTextVector(
            @RequestParam Long chunkId,
            @RequestParam Long userId,
            @RequestParam Long courseId,
            @RequestParam Long documentId,
            @RequestParam(defaultValue = "dev-file.pdf") String fileName,
            @RequestParam(defaultValue = "LECTURE") String documentType,
            @RequestParam(required = false) Integer pageNumber,
            @RequestParam String text
    ) {
        System.out.println("[DevRedisVectorController] Start vector save test.");
        System.out.println("[DevRedisVectorController] chunkId = " + chunkId);
        System.out.println("[DevRedisVectorController] userId = " + userId);
        System.out.println("[DevRedisVectorController] courseId = " + courseId);
        System.out.println("[DevRedisVectorController] documentId = " + documentId);
        System.out.println("[DevRedisVectorController] text length = " + text.length());

        float[] embedding = embeddingService.embed(text);

        DocumentChunk chunk = new DocumentChunk();
        chunk.setId(chunkId);
        chunk.setUserId(userId);
        chunk.setCourseId(courseId);
        chunk.setDocumentId(documentId);
        chunk.setChunkIndex(0);
        chunk.setContent(text);
        chunk.setContentHash(sha256(text));
        chunk.setPageNumber(pageNumber);
        chunk.setSectionTitle(null);
        chunk.setTokenCount(Math.max(1, text.length() / 4));
        chunk.setVectorStatus("PENDING");

        String redisKey = redisVectorService.saveChunkVector(
                chunk,
                fileName,
                documentType,
                embedding
        );

        Map<String, Object> response = new HashMap<>();
        response.put("redisKey", redisKey);
        response.put("embeddingDimension", embedding.length);
        response.put("message", "Saved vector successfully");

        System.out.println("[DevRedisVectorController] Vector save test finished.");
        System.out.println("[DevRedisVectorController] redisKey = " + redisKey);

        return response;
    }

    @GetMapping("/api/dev/vector-search-course")
    public Map<String, Object> searchCourse(
            @RequestParam Long userId,
            @RequestParam Long courseId,
            @RequestParam String question,
            @RequestParam(defaultValue = "5") Integer topK
    ) {
        System.out.println("[DevRedisVectorController] Start vector search test.");
        System.out.println("[DevRedisVectorController] userId = " + userId);
        System.out.println("[DevRedisVectorController] courseId = " + courseId);
        System.out.println("[DevRedisVectorController] question = " + question);
        System.out.println("[DevRedisVectorController] topK = " + topK);

        float[] questionEmbedding = embeddingService.embed(question);

        List<RedisChunkSearchResult> results = redisVectorService.searchCourseChunks(
                userId,
                courseId,
                questionEmbedding,
                topK
        );

        Map<String, Object> response = new HashMap<>();
        response.put("count", results.size());
        response.put("results", results);

        System.out.println("[DevRedisVectorController] Vector search test finished.");
        System.out.println("[DevRedisVectorController] result count = " + results.size());

        return response;
    }

    @GetMapping("/api/dev/vector-search-document")
    public Map<String, Object> searchDocument(
            @RequestParam Long userId,
            @RequestParam Long courseId,
            @RequestParam Long documentId,
            @RequestParam String question,
            @RequestParam(defaultValue = "5") Integer topK
    ) {
        System.out.println("[DevRedisVectorController] Start document vector search test.");
        System.out.println("[DevRedisVectorController] userId = " + userId);
        System.out.println("[DevRedisVectorController] courseId = " + courseId);
        System.out.println("[DevRedisVectorController] documentId = " + documentId);
        System.out.println("[DevRedisVectorController] question = " + question);
        System.out.println("[DevRedisVectorController] topK = " + topK);

        float[] questionEmbedding = embeddingService.embed(question);

        List<RedisChunkSearchResult> results = redisVectorService.searchDocumentChunks(
                userId,
                courseId,
                documentId,
                questionEmbedding,
                topK
        );

        Map<String, Object> response = new HashMap<>();
        response.put("count", results.size());
        response.put("results", results);

        System.out.println("[DevRedisVectorController] Document vector search test finished.");
        System.out.println("[DevRedisVectorController] result count = " + results.size());

        return response;
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