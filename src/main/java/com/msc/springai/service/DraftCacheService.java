package com.msc.springai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.msc.springai.dto.cache.DraftKeyInfo;
import com.msc.springai.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

@Service
@RequiredArgsConstructor
public class DraftCacheService {

    private static final Duration DEFAULT_DRAFT_TTL = Duration.ofDays(7);

    private static final String CACHE_PREFIX = "cache";
    private static final String DRAFT_PART = "draft";

    private static final String SUMMARY_TYPE = "summary";
    private static final String QUIZ_TYPE = "quiz";
    private static final String FLASHCARD_TYPE = "flashcard";

    private static final Set<String> VALID_TYPES = Set.of(
            SUMMARY_TYPE,
            QUIZ_TYPE,
            FLASHCARD_TYPE
    );

    private static final Set<String> VALID_SCOPES = Set.of(
            "COURSE",
            "DOCUMENT"
    );

    private final StringRedisTemplate stringRedisTemplate;

    private final ObjectMapper objectMapper;

    public void saveDraft(
            String key,
            Object value,
            Duration ttl
    ) {
        System.out.println("[DraftCacheService] Try save draft.");
        System.out.println("[DraftCacheService] key = " + key);

        if (value == null) {
            throw new BusinessException(
                    "INVALID_DRAFT_VALUE",
                    "Draft value cannot be null."
            );
        }

        parseDraftKey(key);

        try {
            Duration actualTtl = ttl == null ? DEFAULT_DRAFT_TTL : ttl;
            String json = objectMapper.writeValueAsString(value);

            stringRedisTemplate.opsForValue().set(
                    key,
                    json,
                    actualTtl
            );

            System.out.println("[DraftCacheService] Draft saved.");
            System.out.println("[DraftCacheService] ttlSeconds = " + actualTtl.toSeconds());

        } catch (BusinessException e) {
            throw e;

        } catch (Exception e) {
            System.out.println("[DraftCacheService] Failed to save draft.");
            System.out.println("[DraftCacheService] error = " + e.getMessage());

            throw new BusinessException(
                    "REDIS_DRAFT_SAVE_FAILED",
                    "Failed to save draft to Redis."
            );
        }
    }

    public void saveDraft(
            String key,
            Object value
    ) {
        saveDraft(key, value, DEFAULT_DRAFT_TTL);
    }

    public <T> T getDraft(
            String key,
            Class<T> clazz
    ) {
        System.out.println("[DraftCacheService] Try get draft.");
        System.out.println("[DraftCacheService] key = " + key);

        parseDraftKey(key);

        try {
            String json = stringRedisTemplate.opsForValue().get(key);

            if (json == null || json.isBlank()) {
                System.out.println("[DraftCacheService] Draft not found or expired.");

                throw new BusinessException(
                        "DRAFT_NOT_FOUND",
                        "Draft does not exist or has expired. Please generate again."
                );
            }

            T result = objectMapper.readValue(json, clazz);

            System.out.println("[DraftCacheService] Draft found.");

            return result;

        } catch (BusinessException e) {
            throw e;

        } catch (Exception e) {
            System.out.println("[DraftCacheService] Failed to read draft.");
            System.out.println("[DraftCacheService] error = " + e.getMessage());

            throw new BusinessException(
                    "REDIS_DRAFT_READ_FAILED",
                    "Failed to read draft from Redis."
            );
        }
    }

    public void deleteDraft(String key) {
        System.out.println("[DraftCacheService] Try delete draft.");
        System.out.println("[DraftCacheService] key = " + key);

        parseDraftKey(key);

        try {
            Boolean deleted = stringRedisTemplate.delete(key);

            System.out.println("[DraftCacheService] deleted = " + deleted);

        } catch (Exception e) {
            System.out.println("[DraftCacheService] Failed to delete draft.");
            System.out.println("[DraftCacheService] error = " + e.getMessage());

            throw new BusinessException(
                    "REDIS_DRAFT_DELETE_FAILED",
                    "Failed to delete draft from Redis."
            );
        }
    }

    public String buildSummaryDraftKey(
            Long userId,
            Long courseId,
            String scope,
            Map<String, Object> params
    ) {
        return buildDraftKey(
                SUMMARY_TYPE,
                userId,
                courseId,
                scope,
                params
        );
    }

    public String buildQuizDraftKey(
            Long userId,
            Long courseId,
            String scope,
            Map<String, Object> params
    ) {
        return buildDraftKey(
                QUIZ_TYPE,
                userId,
                courseId,
                scope,
                params
        );
    }

    public String buildFlashcardDraftKey(
            Long userId,
            Long courseId,
            String scope,
            Map<String, Object> params
    ) {
        return buildDraftKey(
                FLASHCARD_TYPE,
                userId,
                courseId,
                scope,
                params
        );
    }

    public void validateDraftOwner(
            String draftKey,
            Long currentUserId
    ) {
        System.out.println("[DraftCacheService] Validate draft owner.");
        System.out.println("[DraftCacheService] draftKey = " + draftKey);
        System.out.println("[DraftCacheService] currentUserId = " + currentUserId);

        if (currentUserId == null) {
            throw new BusinessException(
                    "UNAUTHORIZED",
                    "Current user is required."
            );
        }

        DraftKeyInfo keyInfo = parseDraftKey(draftKey);

        if (!currentUserId.equals(keyInfo.getUserId())) {
            throw new BusinessException(
                    "FORBIDDEN_DRAFT",
                    "You cannot use another user's draft."
            );
        }

        System.out.println("[DraftCacheService] Draft owner verified.");
    }

    public DraftKeyInfo parseDraftKey(String draftKey) {
        if (draftKey == null || draftKey.isBlank()) {
            throw new BusinessException(
                    "INVALID_DRAFT_KEY",
                    "Draft key is required."
            );
        }

        String[] parts = draftKey.split(":");

        /*
         * Expected:
         *
         * cache:summary:draft:{userId}:{courseId}:{scope}:{paramsHash}
         * cache:quiz:draft:{userId}:{courseId}:{scope}:{paramsHash}
         * cache:flashcard:draft:{userId}:{courseId}:{scope}:{paramsHash}
         *
         * parts[0] = cache
         * parts[1] = summary / quiz / flashcard
         * parts[2] = draft
         * parts[3] = userId
         * parts[4] = courseId
         * parts[5] = scope
         * parts[6] = paramsHash
         */
        if (parts.length != 7) {
            throw new BusinessException(
                    "INVALID_DRAFT_KEY",
                    "Invalid draft key format."
            );
        }

        String cachePrefix = parts[0];
        String type = parts[1];
        String draftPart = parts[2];
        String scope = parts[5];
        String paramsHash = parts[6];

        if (!CACHE_PREFIX.equals(cachePrefix)) {
            throw new BusinessException(
                    "INVALID_DRAFT_KEY",
                    "Invalid draft key prefix."
            );
        }

        if (!VALID_TYPES.contains(type)) {
            throw new BusinessException(
                    "INVALID_DRAFT_KEY",
                    "Invalid draft type."
            );
        }

        if (!DRAFT_PART.equals(draftPart)) {
            throw new BusinessException(
                    "INVALID_DRAFT_KEY",
                    "Invalid draft key category."
            );
        }

        if (!VALID_SCOPES.contains(scope)) {
            throw new BusinessException(
                    "INVALID_DRAFT_SCOPE",
                    "Draft scope must be COURSE or DOCUMENT."
            );
        }

        if (paramsHash == null || paramsHash.isBlank()) {
            throw new BusinessException(
                    "INVALID_DRAFT_KEY",
                    "Draft params hash is required."
            );
        }

        Long userId;
        Long courseId;

        try {
            userId = Long.valueOf(parts[3]);
            courseId = Long.valueOf(parts[4]);

        } catch (NumberFormatException e) {
            throw new BusinessException(
                    "INVALID_DRAFT_KEY",
                    "Invalid user id or course id in draft key."
            );
        }

        return new DraftKeyInfo(
                type,
                userId,
                courseId,
                scope,
                paramsHash
        );
    }

    public Long extractUserId(String draftKey) {
        return parseDraftKey(draftKey).getUserId();
    }

    public Long extractCourseId(String draftKey) {
        return parseDraftKey(draftKey).getCourseId();
    }

    public String extractScope(String draftKey) {
        return parseDraftKey(draftKey).getScope();
    }

    public String extractType(String draftKey) {
        return parseDraftKey(draftKey).getType();
    }

    private String buildDraftKey(
            String type,
            Long userId,
            Long courseId,
            String scope,
            Map<String, Object> params
    ) {
        if (!VALID_TYPES.contains(type)) {
            throw new BusinessException(
                    "INVALID_DRAFT_TYPE",
                    "Draft type is invalid."
            );
        }

        if (userId == null) {
            throw new BusinessException(
                    "INVALID_DRAFT_KEY_PARAMS",
                    "User id is required to build draft key."
            );
        }

        if (courseId == null) {
            throw new BusinessException(
                    "INVALID_DRAFT_KEY_PARAMS",
                    "Course id is required to build draft key."
            );
        }

        if (!VALID_SCOPES.contains(scope)) {
            throw new BusinessException(
                    "INVALID_DRAFT_SCOPE",
                    "Draft scope must be COURSE or DOCUMENT."
            );
        }

        String paramsHash = buildParamsHash(params);

        return CACHE_PREFIX
                + ":" + type
                + ":" + DRAFT_PART
                + ":" + userId
                + ":" + courseId
                + ":" + scope
                + ":" + paramsHash;
    }

    private String buildParamsHash(Map<String, Object> params) {
        try {
            Map<String, Object> safeParams = params == null
                    ? Map.of()
                    : new TreeMap<>(params);

            String raw = objectMapper.writeValueAsString(safeParams);

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(raw.getBytes(StandardCharsets.UTF_8));

            String fullHash = HexFormat.of().formatHex(hashBytes);

            return fullHash.substring(0, 6);

        } catch (Exception e) {
            throw new BusinessException(
                    "PARAM_HASH_FAILED",
                    "Failed to generate draft parameter hash."
            );
        }
    }
}