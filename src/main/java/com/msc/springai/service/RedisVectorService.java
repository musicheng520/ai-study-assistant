package com.msc.springai.service;

import com.msc.springai.dto.document.RedisChunkSearchResult;
import com.msc.springai.entity.DocumentChunk;
import com.msc.springai.mapper.DocumentChunkMapper;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.output.NestedMultiOutput;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.ProtocolKeyword;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Service;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RedisVectorService {

    private final RedisConnectionFactory redisConnectionFactory;
    private final DocumentChunkMapper documentChunkMapper;

    @Value("${app.vector.redis-index-name}")
    private String indexName;

    @Value("${app.vector.redis-key-prefix}")
    private String keyPrefix;

    @Value("${app.vector.embedding-model}")
    private String embeddingModel;

    @Value("${app.vector.embedding-dimension}")
    private Integer embeddingDimension;

    @PostConstruct
    public void init() {
        System.out.println("[RedisVectorService] Initializing Redis Vector Service.");
        System.out.println("[RedisVectorService] indexName = " + indexName);
        System.out.println("[RedisVectorService] keyPrefix = " + keyPrefix);
        System.out.println("[RedisVectorService] embeddingModel = " + embeddingModel);
        System.out.println("[RedisVectorService] embeddingDimension = " + embeddingDimension);

        ensureIndex();
    }

    /**
     * 保存一个 chunk 的 embedding 到 Redis Hash。
     *
     * Redis key:
     * vector:chunk:{chunkId}
     */
    public String saveChunkVector(
            DocumentChunk chunk,
            String fileName,
            String documentType,
            float[] embedding
    ) {
        System.out.println("[RedisVectorService] Start saving chunk vector.");

        if (chunk == null) {
            throw new RuntimeException("DocumentChunk is required");
        }

        if (chunk.getId() == null) {
            throw new RuntimeException("Chunk id is required before saving vector");
        }

        if (embedding == null || embedding.length == 0) {
            throw new RuntimeException("Embedding is empty");
        }

        if (!embeddingDimension.equals(embedding.length)) {
            throw new RuntimeException("Embedding dimension mismatch. expected="
                    + embeddingDimension + ", actual=" + embedding.length);
        }

        String redisKey = keyPrefix + chunk.getId();

        System.out.println("[RedisVectorService] redisKey = " + redisKey);
        System.out.println("[RedisVectorService] chunkId = " + chunk.getId());
        System.out.println("[RedisVectorService] userId = " + chunk.getUserId());
        System.out.println("[RedisVectorService] courseId = " + chunk.getCourseId());
        System.out.println("[RedisVectorService] documentId = " + chunk.getDocumentId());
        System.out.println("[RedisVectorService] fileName = " + fileName);
        System.out.println("[RedisVectorService] documentType = " + documentType);
        System.out.println("[RedisVectorService] content length = "
                + (chunk.getContent() == null ? 0 : chunk.getContent().length()));
        System.out.println("[RedisVectorService] embedding length = " + embedding.length);

        try (RedisConnection connection = redisConnectionFactory.getConnection()) {
            byte[] keyBytes = bytes(redisKey);

            hSetString(connection, keyBytes, "chunkId", chunk.getId());
            hSetString(connection, keyBytes, "userId", chunk.getUserId());
            hSetString(connection, keyBytes, "courseId", chunk.getCourseId());
            hSetString(connection, keyBytes, "documentId", chunk.getDocumentId());
            hSetString(connection, keyBytes, "documentType", documentType);
            hSetString(connection, keyBytes, "fileName", fileName);
            hSetString(connection, keyBytes, "pageNumber", chunk.getPageNumber());
            hSetString(connection, keyBytes, "sectionTitle", chunk.getSectionTitle());
            hSetString(connection, keyBytes, "content", chunk.getContent());
            hSetString(connection, keyBytes, "contentHash", chunk.getContentHash());
            hSetString(connection, keyBytes, "createdAt", LocalDateTime.now().toString());

            hSetBinary(connection, keyBytes, "embedding", floatArrayToBytes(embedding));

            System.out.println("[RedisVectorService] Redis HSET finished.");

            int updated = documentChunkMapper.updateVectorStatus(
                    chunk.getId(),
                    chunk.getUserId(),
                    redisKey,
                    "READY",
                    embeddingModel,
                    embedding.length
            );

            System.out.println("[RedisVectorService] MySQL vector status update count = " + updated);

            if (updated == 0) {
                System.out.println("[RedisVectorService] Warning: MySQL chunk was not updated. "
                        + "This is normal if you are using dev test chunkId that does not exist in document_chunks.");
            }

            System.out.println("[RedisVectorService] Save chunk vector success.");

            return redisKey;

        } catch (Exception e) {
            System.out.println("[RedisVectorService] Save chunk vector failed: " + e.getMessage());

            try {
                documentChunkMapper.updateVectorStatus(
                        chunk.getId(),
                        chunk.getUserId(),
                        null,
                        "FAILED",
                        embeddingModel,
                        embedding.length
                );
            } catch (Exception updateException) {
                System.out.println("[RedisVectorService] Failed to mark chunk vector FAILED: "
                        + updateException.getMessage());
            }

            throw new RuntimeException("Failed to save chunk vector: " + e.getMessage(), e);
        }
    }

    /**
     * Course-level vector search.
     *
     * filter:
     * userId = currentUserId AND courseId = targetCourseId
     */
    public List<RedisChunkSearchResult> searchCourseChunks(
            Long userId,
            Long courseId,
            float[] questionEmbedding,
            int topK
    ) {
        System.out.println("[RedisVectorService] Start course vector search.");
        System.out.println("[RedisVectorService] userId = " + userId);
        System.out.println("[RedisVectorService] courseId = " + courseId);
        System.out.println("[RedisVectorService] topK = " + topK);

        String query = "(@userId:[" + userId + " " + userId + "] "
                + "@courseId:[" + courseId + " " + courseId + "])"
                + "=>[KNN " + topK + " @embedding $vec AS distance]";

        return search(query, questionEmbedding, topK);
    }

    /**
     * Document-level vector search.
     *
     * filter:
     * userId = currentUserId AND courseId = targetCourseId AND documentId = targetDocumentId
     */
    public List<RedisChunkSearchResult> searchDocumentChunks(
            Long userId,
            Long courseId,
            Long documentId,
            float[] questionEmbedding,
            int topK
    ) {
        System.out.println("[RedisVectorService] Start document vector search.");
        System.out.println("[RedisVectorService] userId = " + userId);
        System.out.println("[RedisVectorService] courseId = " + courseId);
        System.out.println("[RedisVectorService] documentId = " + documentId);
        System.out.println("[RedisVectorService] topK = " + topK);

        String query = "(@userId:[" + userId + " " + userId + "] "
                + "@courseId:[" + courseId + " " + courseId + "] "
                + "@documentId:[" + documentId + " " + documentId + "])"
                + "=>[KNN " + topK + " @embedding $vec AS distance]";

        return search(query, questionEmbedding, topK);
    }

    /**
     * 删除某个 document 下所有 chunk vectors。
     */
    public void deleteDocumentVectors(Long userId, Long documentId) {
        System.out.println("[RedisVectorService] Start deleting document vectors.");
        System.out.println("[RedisVectorService] userId = " + userId);
        System.out.println("[RedisVectorService] documentId = " + documentId);

        List<DocumentChunk> chunks = documentChunkMapper.findByDocumentIdAndUserId(
                documentId,
                userId
        );

        System.out.println("[RedisVectorService] Chunks to delete = " + chunks.size());

        try (RedisConnection connection = redisConnectionFactory.getConnection()) {
            for (DocumentChunk chunk : chunks) {
                String redisKey = chunk.getVectorKey();

                if (redisKey == null || redisKey.isBlank()) {
                    redisKey = keyPrefix + chunk.getId();
                }

                Long deleted = connection.del(bytes(redisKey));

                System.out.println("[RedisVectorService] Deleted redis key = "
                        + redisKey + ", result = " + deleted);
            }
        }

        System.out.println("[RedisVectorService] Delete document vectors finished.");
    }

    /**
     * 真正执行 RediSearch vector search。
     *
     * 这里不能用普通 connection.execute("FT.SEARCH", ...)
     * 因为 FT.SEARCH 返回 Long + byte[] + List 的混合结构，
     * 普通 ByteArrayOutput 解析不了，会报：
     * ByteArrayOutput does not support set(long)
     */
    private List<RedisChunkSearchResult> search(
            String query,
            float[] questionEmbedding,
            int topK
    ) {
        if (questionEmbedding == null || questionEmbedding.length == 0) {
            throw new RuntimeException("Question embedding is empty");
        }

        if (!embeddingDimension.equals(questionEmbedding.length)) {
            throw new RuntimeException("Question embedding dimension mismatch. expected="
                    + embeddingDimension + ", actual=" + questionEmbedding.length);
        }

        System.out.println("[RedisVectorService] RediSearch query = " + query);
        System.out.println("[RedisVectorService] Question embedding dimension = "
                + questionEmbedding.length);

        try {
            byte[] vectorBytes = floatArrayToBytes(questionEmbedding);

            Object raw = executeNestedRedisCommand(
                    "FT.SEARCH",
                    bytes(indexName),
                    bytes(query),

                    bytes("PARAMS"),
                    bytes("2"),
                    bytes("vec"),
                    vectorBytes,

                    bytes("RETURN"),
                    bytes("10"),
                    bytes("chunkId"),
                    bytes("userId"),
                    bytes("courseId"),
                    bytes("documentId"),
                    bytes("fileName"),
                    bytes("pageNumber"),
                    bytes("sectionTitle"),
                    bytes("content"),
                    bytes("distance"),
                    bytes("contentHash"),

                    bytes("SORTBY"),
                    bytes("distance"),
                    bytes("ASC"),

                    bytes("DIALECT"),
                    bytes("2")
            );

            List<RedisChunkSearchResult> results = parseSearchResults(raw);

            System.out.println("[RedisVectorService] Search result count = " + results.size());

            return results;

        } catch (Exception e) {
            System.out.println("[RedisVectorService] Vector search failed: " + e.getMessage());
            throw new RuntimeException("Vector search failed: " + e.getMessage(), e);
        }
    }

    /**
     * 启动时确保 Redis vector index 存在。
     */
    private void ensureIndex() {
        System.out.println("[RedisVectorService] Checking Redis vector index.");

        try {
            Object indexListRaw = executeNestedRedisCommand("FT._LIST");

            if (isIndexExists(indexListRaw, indexName)) {
                System.out.println("[RedisVectorService] Index already exists: " + indexName);
                return;
            }

            System.out.println("[RedisVectorService] Index does not exist, creating: " + indexName);

            try (RedisConnection connection = redisConnectionFactory.getConnection()) {
                Object response = connection.execute(
                        "FT.CREATE",
                        bytes(indexName),
                        bytes("ON"),
                        bytes("HASH"),
                        bytes("PREFIX"),
                        bytes("1"),
                        bytes(keyPrefix),
                        bytes("SCHEMA"),

                        bytes("chunkId"),
                        bytes("NUMERIC"),
                        bytes("SORTABLE"),

                        bytes("userId"),
                        bytes("NUMERIC"),
                        bytes("SORTABLE"),

                        bytes("courseId"),
                        bytes("NUMERIC"),
                        bytes("SORTABLE"),

                        bytes("documentId"),
                        bytes("NUMERIC"),
                        bytes("SORTABLE"),

                        bytes("documentType"),
                        bytes("TAG"),

                        bytes("fileName"),
                        bytes("TEXT"),

                        bytes("pageNumber"),
                        bytes("NUMERIC"),

                        bytes("sectionTitle"),
                        bytes("TEXT"),

                        bytes("content"),
                        bytes("TEXT"),

                        bytes("contentHash"),
                        bytes("TAG"),

                        bytes("embedding"),
                        bytes("VECTOR"),
                        bytes("HNSW"),
                        bytes("6"),
                        bytes("TYPE"),
                        bytes("FLOAT32"),
                        bytes("DIM"),
                        bytes(String.valueOf(embeddingDimension)),
                        bytes("DISTANCE_METRIC"),
                        bytes("COSINE")
                );

                System.out.println("[RedisVectorService] Index create response = "
                        + asString(response));
            }

        } catch (Exception e) {
            System.out.println("[RedisVectorService] Failed to create Redis vector index: "
                    + e.getMessage());

            throw new RuntimeException("Failed to create Redis vector index: "
                    + e.getMessage(), e);
        }
    }

    private boolean isIndexExists(Object raw, String targetIndexName) {
        if (!(raw instanceof List<?> list)) {
            System.out.println("[RedisVectorService] FT._LIST raw result is not list: " + raw);
            return false;
        }

        for (Object item : list) {
            String existingIndex = asString(item);

            System.out.println("[RedisVectorService] Existing index: " + existingIndex);

            if (targetIndexName.equals(existingIndex)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 用 Lettuce NestedMultiOutput 执行 RediSearch 复杂返回值命令。
     */
    @SuppressWarnings("unchecked")
    private Object executeNestedRedisCommand(String commandName, byte[]... args) {
        System.out.println("[RedisVectorService] Execute nested redis command: " + commandName);

        try (RedisConnection springConnection = redisConnectionFactory.getConnection()) {
            Object nativeConnection = springConnection.getNativeConnection();

            System.out.println("[RedisVectorService] Native connection class = "
                    + nativeConnection.getClass().getName());

            if (!(nativeConnection instanceof RedisAsyncCommands<?, ?>)) {
                throw new RuntimeException(
                        "Native Redis connection is not RedisAsyncCommands. actual="
                                + nativeConnection.getClass().getName()
                );
            }

            RedisAsyncCommands<byte[], byte[]> asyncCommands =
                    (RedisAsyncCommands<byte[], byte[]>) nativeConnection;

            CommandArgs<byte[], byte[]> commandArgs =
                    new CommandArgs<>(ByteArrayCodec.INSTANCE);

            for (byte[] arg : args) {
                commandArgs.add(arg);
            }

            RedisFuture<?> future = asyncCommands.dispatch(
                    new RawRedisCommand(commandName),
                    new NestedMultiOutput<>(ByteArrayCodec.INSTANCE),
                    commandArgs
            );

            Object result = future.get(10, TimeUnit.SECONDS);

            System.out.println("[RedisVectorService] Nested redis command finished: "
                    + commandName);

            return result;

        } catch (Exception e) {
            System.out.println("[RedisVectorService] Nested redis command failed: "
                    + commandName + ", error = " + e.getMessage());

            throw new RuntimeException(
                    "Nested redis command failed: " + commandName + ", error=" + e.getMessage(),
                    e
            );
        }
    }
    /**
     * 解析 FT.SEARCH 的返回结果。
     *
     * 返回结构类似：
     * [
     *   totalCount,
     *   redisKey,
     *   [field, value, field, value ...],
     *   redisKey,
     *   [field, value, field, value ...]
     * ]
     */
    private List<RedisChunkSearchResult> parseSearchResults(Object raw) {
        System.out.println("[RedisVectorService] Start parsing search results.");

        if (!(raw instanceof List<?> rawList)) {
            System.out.println("[RedisVectorService] Raw result is not a list: " + raw);
            return new ArrayList<>();
        }

        if (rawList.isEmpty()) {
            System.out.println("[RedisVectorService] Raw result list is empty.");
            return new ArrayList<>();
        }

        System.out.println("[RedisVectorService] Raw list size = " + rawList.size());

        // Redis Stack / Lettuce RESP3 format:
        // [attributes, [], format, STRING, results, [...], total_results, 1]
        if (containsKey(rawList, "results")) {
            System.out.println("[RedisVectorService] Detected RESP3 RediSearch result format.");
            return parseResp3SearchResults(rawList);
        }

        // Old RESP2 format:
        // [totalCount, redisKey, [field, value, field, value]]
        System.out.println("[RedisVectorService] Detected RESP2 RediSearch result format.");
        return parseResp2SearchResults(rawList);
    }

    private List<RedisChunkSearchResult> parseResp3SearchResults(List<?> rawList) {
        List<RedisChunkSearchResult> results = new ArrayList<>();

        Object totalResultsObject = valueAfterKey(rawList, "total_results");
        System.out.println("[RedisVectorService] RESP3 total_results = "
                + asString(totalResultsObject));

        Object resultsObject = valueAfterKey(rawList, "results");

        if (!(resultsObject instanceof List<?> resultItems)) {
            System.out.println("[RedisVectorService] RESP3 results object is not list: "
                    + resultsObject);
            return results;
        }

        System.out.println("[RedisVectorService] RESP3 result item count = "
                + resultItems.size());

        for (Object item : resultItems) {
            RedisChunkSearchResult result = parseResp3ResultItem(item);

            if (result != null) {
                results.add(result);

                System.out.println("[RedisVectorService] Parsed RESP3 result: key="
                        + result.getRedisKey()
                        + ", chunkId=" + result.getChunkId()
                        + ", distance=" + result.getDistance());
            }
        }

        return results;
    }

    private RedisChunkSearchResult parseResp3ResultItem(Object item) {
        if (!(item instanceof List<?> itemList)) {
            System.out.println("[RedisVectorService] RESP3 result item is not list: " + item);
            return null;
        }

        String redisKey = null;
        Map<String, String> fieldMap = new HashMap<>();

        for (int i = 0; i < itemList.size(); i += 2) {
            if (i + 1 >= itemList.size()) {
                break;
            }

            String key = asString(itemList.get(i));
            Object value = itemList.get(i + 1);

            if ("id".equals(key)) {
                redisKey = asString(value);
                continue;
            }

            if ("extra_attributes".equals(key) || "values".equals(key)) {
                Map<String, String> attributes = parseFieldList(value);
                fieldMap.putAll(attributes);
            }
        }

        if (redisKey == null || redisKey.isBlank()) {
            System.out.println("[RedisVectorService] RESP3 result redis key is empty.");
            return null;
        }

        RedisChunkSearchResult result = new RedisChunkSearchResult();

        result.setRedisKey(redisKey);
        result.setChunkId(parseLong(fieldMap.get("chunkId")));
        result.setUserId(parseLong(fieldMap.get("userId")));
        result.setCourseId(parseLong(fieldMap.get("courseId")));
        result.setDocumentId(parseLong(fieldMap.get("documentId")));
        result.setFileName(fieldMap.get("fileName"));
        result.setPageNumber(parseInteger(fieldMap.get("pageNumber")));
        result.setSectionTitle(fieldMap.get("sectionTitle"));
        result.setContent(fieldMap.get("content"));
        result.setDistance(parseDouble(fieldMap.get("distance")));

        return result;
    }

    private List<RedisChunkSearchResult> parseResp2SearchResults(List<?> rawList) {
        List<RedisChunkSearchResult> results = new ArrayList<>();

        System.out.println("[RedisVectorService] RESP2 raw total result count = "
                + asString(rawList.get(0)));

        for (int i = 1; i < rawList.size(); i += 2) {
            String redisKey = asString(rawList.get(i));

            if (i + 1 >= rawList.size()) {
                break;
            }

            Object fieldsObject = rawList.get(i + 1);

            Map<String, String> fieldMap = parseFieldList(fieldsObject);

            RedisChunkSearchResult result = new RedisChunkSearchResult();

            result.setRedisKey(redisKey);
            result.setChunkId(parseLong(fieldMap.get("chunkId")));
            result.setUserId(parseLong(fieldMap.get("userId")));
            result.setCourseId(parseLong(fieldMap.get("courseId")));
            result.setDocumentId(parseLong(fieldMap.get("documentId")));
            result.setFileName(fieldMap.get("fileName"));
            result.setPageNumber(parseInteger(fieldMap.get("pageNumber")));
            result.setSectionTitle(fieldMap.get("sectionTitle"));
            result.setContent(fieldMap.get("content"));
            result.setDistance(parseDouble(fieldMap.get("distance")));

            results.add(result);

            System.out.println("[RedisVectorService] Parsed RESP2 result: key="
                    + redisKey
                    + ", chunkId=" + result.getChunkId()
                    + ", distance=" + result.getDistance());
        }

        return results;
    }
    private Map<String, String> parseFieldList(Object fieldsObject) {
        Map<String, String> fieldMap = new HashMap<>();

        if (!(fieldsObject instanceof List<?> fieldList)) {
            System.out.println("[RedisVectorService] Field object is not list: " + fieldsObject);
            return fieldMap;
        }

        for (int i = 0; i < fieldList.size(); i += 2) {
            if (i + 1 >= fieldList.size()) {
                break;
            }

            String fieldName = asString(fieldList.get(i));
            String fieldValue = asString(fieldList.get(i + 1));

            fieldMap.put(fieldName, fieldValue);

            System.out.println("[RedisVectorService] Field parsed: "
                    + fieldName + " = "
                    + previewValue(fieldValue));
        }

        return fieldMap;
    }
    private boolean containsKey(List<?> list, String targetKey) {
        for (Object item : list) {
            String value = asString(item);

            if (targetKey.equals(value)) {
                return true;
            }
        }

        return false;
    }

    private Object valueAfterKey(List<?> list, String targetKey) {
        for (int i = 0; i < list.size() - 1; i++) {
            String key = asString(list.get(i));

            if (targetKey.equals(key)) {
                return list.get(i + 1);
            }
        }

        return null;
    }
    private String previewValue(String value) {
        if (value == null) {
            return null;
        }

        if (value.length() <= 120) {
            return value;
        }

        return value.substring(0, 120) + "...";
    }

    private byte[] floatArrayToBytes(float[] vector) {
        ByteBuffer buffer = ByteBuffer.allocate(vector.length * Float.BYTES);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        for (float value : vector) {
            buffer.putFloat(value);
        }

        return buffer.array();
    }

    private void hSetString(
            RedisConnection connection,
            byte[] key,
            String field,
            Object value
    ) {
        if (value == null) {
            return;
        }

        String stringValue = String.valueOf(value);

        if (stringValue.isBlank()) {
            return;
        }

        connection.hSet(key, bytes(field), bytes(stringValue));
    }

    private void hSetBinary(
            RedisConnection connection,
            byte[] key,
            String field,
            byte[] value
    ) {
        if (value == null || value.length == 0) {
            return;
        }

        connection.hSet(key, bytes(field), value);
    }

    private byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    private String asString(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof byte[] bytes) {
            return new String(bytes, StandardCharsets.UTF_8);
        }

        return String.valueOf(value);
    }

    private Long parseLong(String value) {
        try {
            if (value == null || value.isBlank()) {
                return null;
            }

            return Long.parseLong(value);
        } catch (Exception e) {
            return null;
        }
    }

    private Integer parseInteger(String value) {
        try {
            if (value == null || value.isBlank()) {
                return null;
            }

            return Integer.parseInt(value);
        } catch (Exception e) {
            return null;
        }
    }

    private Double parseDouble(String value) {
        try {
            if (value == null || value.isBlank()) {
                return null;
            }

            return Double.parseDouble(value);
        } catch (Exception e) {
            return null;
        }
    }

    private static class RawRedisCommand implements ProtocolKeyword {

        private final String name;

        private RawRedisCommand(String name) {
            this.name = name;
        }

        @Override
        public byte[] getBytes() {
            return name.getBytes(StandardCharsets.UTF_8);
        }

        @Override
        public String name() {
            return name;
        }
    }
}