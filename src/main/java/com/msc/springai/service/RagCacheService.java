package com.msc.springai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.msc.springai.dto.rag.CachedRagResult;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RagCacheService {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.rag.cache.enabled:true}")
    private Boolean cacheEnabled;

    @Value("${app.rag.cache.ttl-minutes:30}")
    private Long ttlMinutes;

    @Value("${app.rag.cache.key-prefix:rag:course:}")
    private String keyPrefix;

    public CachedRagResult getCourseRagCache(
            Long userId,
            Long courseId,
            String question,
            Integer topK
    ) {
        System.out.println("[RagCacheService] Try get RAG cache.");

        if (!Boolean.TRUE.equals(cacheEnabled)) {
            System.out.println("[RagCacheService] Cache disabled.");
            return null;
        }

        String key = buildCourseRagCacheKey(
                userId,
                courseId,
                question,
                topK
        );

        System.out.println("[RagCacheService] cacheKey = " + key);

        try {
            String json = stringRedisTemplate.opsForValue().get(key);

            if (json == null || json.isBlank()) {
                System.out.println("[RagCacheService] Cache MISS.");
                return null;
            }

            CachedRagResult cachedResult = objectMapper.readValue(
                    json,
                    CachedRagResult.class
            );

            System.out.println("[RagCacheService] Cache HIT.");
            System.out.println("[RagCacheService] cached answer length = "
                    + (cachedResult.getAnswer() == null ? 0 : cachedResult.getAnswer().length()));
            System.out.println("[RagCacheService] cached citation count = "
                    + (cachedResult.getCitations() == null ? 0 : cachedResult.getCitations().size()));

            return cachedResult;

        } catch (Exception e) {
            System.out.println("[RagCacheService] Failed to read RAG cache: "
                    + e.getMessage());

            return null;
        }
    }

    public void saveCourseRagCache(
            Long userId,
            Long courseId,
            String question,
            Integer topK,
            CachedRagResult result
    ) {
        System.out.println("[RagCacheService] Try save RAG cache.");

        if (!Boolean.TRUE.equals(cacheEnabled)) {
            System.out.println("[RagCacheService] Cache disabled.");
            return;
        }

        if (result == null) {
            System.out.println("[RagCacheService] Cache result is null, skip.");
            return;
        }

        String key = buildCourseRagCacheKey(
                userId,
                courseId,
                question,
                topK
        );

        System.out.println("[RagCacheService] cacheKey = " + key);

        try {
            String json = objectMapper.writeValueAsString(result);

            stringRedisTemplate.opsForValue().set(
                    key,
                    json,
                    Duration.ofMinutes(ttlMinutes)
            );

            System.out.println("[RagCacheService] RAG cache saved.");
            System.out.println("[RagCacheService] ttlMinutes = " + ttlMinutes);

        } catch (Exception e) {
            System.out.println("[RagCacheService] Failed to save RAG cache: "
                    + e.getMessage());
        }
    }

    public void evictCourseRagCache(
            Long userId,
            Long courseId
    ) {
        System.out.println("[RagCacheService] Try evict course RAG cache.");
        System.out.println("[RagCacheService] userId = " + userId);
        System.out.println("[RagCacheService] courseId = " + courseId);

        if (!Boolean.TRUE.equals(cacheEnabled)) {
            System.out.println("[RagCacheService] Cache disabled.");
            return;
        }

        if (userId == null || courseId == null) {
            System.out.println("[RagCacheService] userId or courseId is null, skip evict.");
            return;
        }

        String pattern = keyPrefix
                + "user:" + userId
                + ":course:" + courseId
                + ":*";

        System.out.println("[RagCacheService] evict pattern = " + pattern);

        try {
            Set<String> keys = stringRedisTemplate.keys(pattern);

            if (keys == null || keys.isEmpty()) {
                System.out.println("[RagCacheService] No cache keys to evict.");
                return;
            }

            Long deleted = stringRedisTemplate.delete(keys);

            System.out.println("[RagCacheService] Evicted cache key count = " + deleted);

        } catch (Exception e) {
            System.out.println("[RagCacheService] Failed to evict RAG cache: "
                    + e.getMessage());
        }
    }

    private String buildCourseRagCacheKey(
            Long userId,
            Long courseId,
            String question,
            Integer topK
    ) {
        if (userId == null) {
            throw new RuntimeException("User id is required to build RAG cache key");
        }

        if (courseId == null) {
            throw new RuntimeException("Course id is required to build RAG cache key");
        }

        String normalizedQuestion = normalizeQuestion(question);
        String questionHash = sha256(normalizedQuestion);
        int normalizedTopK = topK == null ? 5 : topK;

        return keyPrefix
                + "user:" + userId
                + ":course:" + courseId
                + ":topK:" + normalizedTopK
                + ":q:" + questionHash;
    }

    private String normalizeQuestion(String question) {
        if (question == null) {
            return "";
        }

        return question
                .replaceAll("\\s+", " ")
                .trim()
                .toLowerCase();
    }

    private String sha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate cache hash");
        }
    }
}