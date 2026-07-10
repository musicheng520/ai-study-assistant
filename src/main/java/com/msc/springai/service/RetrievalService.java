package com.msc.springai.service;

import com.msc.springai.dto.document.RedisChunkSearchResult;
import com.msc.springai.dto.learning.retrieval.RetrievedChunk;
import com.msc.springai.dto.workflow.tool.StudyToolDtos;
import com.msc.springai.entity.Course;
import com.msc.springai.entity.CourseDocument;
import com.msc.springai.exception.BusinessException;
import com.msc.springai.mapper.CourseDocumentMapper;
import com.msc.springai.mapper.CourseMapper;
import com.msc.springai.service.tool.DocumentSearchToolGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class RetrievalService implements DocumentSearchToolGateway {

    private static final String DOCUMENT_STATUS_READY = "READY";

    private final CourseMapper courseMapper;

    private final CourseDocumentMapper courseDocumentMapper;

    private final EmbeddingService embeddingService;

    private final RedisVectorService redisVectorService;


    @Value("${app.retrieval.default-top-k:8}")
    private Integer defaultTopK;

    @Value("${app.retrieval.similarity-threshold:0.60}")
    private Double similarityThreshold;

    @Value("${app.retrieval.course-generation-query:Summarize the main topics from this course.}")
    private String courseGenerationQuery;

    @Value("${app.retrieval.document-generation-query:Summarize the main topics from this document.}")
    private String documentGenerationQuery;


    public List<RetrievedChunk> retrieveCourseChunks(
            Long userId,
            Long courseId,
            Integer topK
    ) {
        return retrieveCourseChunks(
                userId,
                courseId,
                topK,
                courseGenerationQuery
        );
    }

    public List<RetrievedChunk> retrieveCourseChunks(
            Long userId,
            Long courseId,
            Integer topK,
            String retrievalQuery
    ) {
        System.out.println("[RetrievalService] Start course-level retrieval.");
        System.out.println("[RetrievalService] userId = " + userId);
        System.out.println("[RetrievalService] courseId = " + courseId);

        validateUserId(userId);
        validateCourseId(courseId);

        Course course = courseMapper.findByIdAndUserId(courseId, userId);

        if (course == null) {
            throw new BusinessException(
                    "COURSE_NOT_FOUND",
                    "Course not found or access denied."
            );
        }

        int readyDocumentCount = courseDocumentMapper.countReadyByUserIdAndCourseId(
                userId,
                courseId
        );

        if (readyDocumentCount <= 0) {
            throw new BusinessException(
                    "NO_READY_DOCUMENTS",
                    "No ready documents found in this course."
            );
        }

        int normalizedTopK = normalizeTopK(topK);
        String query = normalizeRetrievalQuery(
                retrievalQuery,
                courseGenerationQuery
        );

        System.out.println("[RetrievalService] readyDocumentCount = " + readyDocumentCount);
        System.out.println("[RetrievalService] topK = " + normalizedTopK);
        System.out.println("[RetrievalService] similarityThreshold = " + similarityThreshold);
        System.out.println("[RetrievalService] query = " + query);

        float[] queryEmbedding = embeddingService.embed(query);

        List<RedisChunkSearchResult> rawChunks =
                redisVectorService.searchCourseChunks(
                        userId,
                        courseId,
                        queryEmbedding,
                        normalizedTopK
                );

        List<RetrievedChunk> chunks = filterAndMapChunks(
                userId,
                courseId,
                null,
                rawChunks,
                normalizedTopK
        );

        if (chunks.isEmpty()) {
            throw new BusinessException(
                    "NO_RELEVANT_CHUNKS",
                    "No relevant chunks found for this course."
            );
        }

        System.out.println("[RetrievalService] returnedChunkCount = " + chunks.size());

        return chunks;
    }

    public List<RetrievedChunk> retrieveDocumentChunks(
            Long userId,
            Long courseId,
            Long documentId,
            Integer topK
    ) {
        return retrieveDocumentChunks(
                userId,
                courseId,
                documentId,
                topK,
                documentGenerationQuery
        );
    }

    public List<RetrievedChunk> retrieveDocumentChunks(
            Long userId,
            Long courseId,
            Long documentId,
            Integer topK,
            String retrievalQuery
    ) {
        System.out.println("[RetrievalService] Start document-level retrieval.");
        System.out.println("[RetrievalService] userId = " + userId);
        System.out.println("[RetrievalService] courseId = " + courseId);
        System.out.println("[RetrievalService] documentId = " + documentId);

        validateUserId(userId);
        validateCourseId(courseId);

        if (documentId == null) {
            throw new BusinessException(
                    "INVALID_DOCUMENT_ID",
                    "Document id is required."
            );
        }

        Course course = courseMapper.findByIdAndUserId(courseId, userId);

        if (course == null) {
            throw new BusinessException(
                    "COURSE_NOT_FOUND",
                    "Course not found or access denied."
            );
        }

        CourseDocument document = courseDocumentMapper.findByIdAndUserId(
                documentId,
                userId
        );

        if (document == null) {
            throw new BusinessException(
                    "DOCUMENT_NOT_FOUND",
                    "Document not found or access denied."
            );
        }

        if (!Objects.equals(document.getCourseId(), courseId)) {
            throw new BusinessException(
                    "DOCUMENT_ACCESS_DENIED",
                    "Document does not belong to this course."
            );
        }

        if (!DOCUMENT_STATUS_READY.equals(document.getStatus())) {
            throw new BusinessException(
                    "DOCUMENT_NOT_READY",
                    "Document is not ready for generation."
            );
        }

        int normalizedTopK = normalizeTopK(topK);
        String query = normalizeRetrievalQuery(
                retrievalQuery,
                documentGenerationQuery
        );

        System.out.println("[RetrievalService] document status verified.");
        System.out.println("[RetrievalService] topK = " + normalizedTopK);
        System.out.println("[RetrievalService] similarityThreshold = " + similarityThreshold);
        System.out.println("[RetrievalService] query = " + query);

        float[] queryEmbedding = embeddingService.embed(query);

        /*
         * Day 3 MVP:
         * 先复用 course-level Redis vector search，
         * 然后在 Java 层严格过滤 documentId。
         *
         * 如果你的 RedisVectorService 已经有 searchDocumentChunks，
         * 后面可以直接换成 document-level vector filter。
         */
        int searchTopK = Math.max(normalizedTopK * 5, 20);

        List<RedisChunkSearchResult> rawChunks =
                redisVectorService.searchCourseChunks(
                        userId,
                        courseId,
                        queryEmbedding,
                        searchTopK
                );

        List<RetrievedChunk> chunks = filterAndMapChunks(
                userId,
                courseId,
                documentId,
                rawChunks,
                normalizedTopK
        );

        if (chunks.isEmpty()) {
            throw new BusinessException(
                    "NO_RELEVANT_CHUNKS",
                    "No relevant chunks found for this document."
            );
        }

        System.out.println("[RetrievalService] returnedChunkCount = " + chunks.size());

        return chunks;
    }

    private List<RetrievedChunk> filterAndMapChunks(
            Long userId,
            Long courseId,
            Long documentId,
            List<RedisChunkSearchResult> rawChunks,
            int limit
    ) {
        List<RetrievedChunk> result = new ArrayList<>();

        if (rawChunks == null || rawChunks.isEmpty()) {
            System.out.println("[RetrievalService] rawChunks is empty.");
            return result;
        }

        System.out.println("[RetrievalService] rawChunks count = " + rawChunks.size());

        for (RedisChunkSearchResult rawChunk : rawChunks) {
            if (rawChunk == null) {
                continue;
            }

            System.out.println("[RetrievalService] Raw chunk:");
            System.out.println("  chunkId = " + rawChunk.getChunkId());
            System.out.println("  userId = " + rawChunk.getUserId());
            System.out.println("  courseId = " + rawChunk.getCourseId());
            System.out.println("  documentId = " + rawChunk.getDocumentId());
            System.out.println("  distance = " + rawChunk.getDistance());
            System.out.println("  contentLength = " + (
                    rawChunk.getContent() == null ? 0 : rawChunk.getContent().length()
            ));

            if (rawChunk.getChunkId() == null) {
                System.out.println("[RetrievalService] Skip: chunkId is null.");
                continue;
            }

            if (!Objects.equals(rawChunk.getUserId(), userId)) {
                System.out.println("[RetrievalService] Skip: userId mismatch.");
                continue;
            }

            if (!Objects.equals(rawChunk.getCourseId(), courseId)) {
                System.out.println("[RetrievalService] Skip: courseId mismatch.");
                continue;
            }

            if (documentId != null && !Objects.equals(rawChunk.getDocumentId(), documentId)) {
                System.out.println("[RetrievalService] Skip: documentId mismatch.");
                continue;
            }

            if (rawChunk.getContent() == null || rawChunk.getContent().isBlank()) {
                System.out.println("[RetrievalService] Skip: content is blank.");
                continue;
            }

            /*
             * Redis Vector 返回 distance：
             * distance 越小越相关。
             *
             * RetrievedChunk 统一返回 score：
             * score 越大越相关。
             *
             * 使用 1 / (1 + distance)，避免 distance > 1 时出现负数。
             */
            Double distance = rawChunk.getDistance();
            Double score = null;

            if (distance != null) {
                score = 1.0 / (1.0 + distance);
            }

            System.out.println("[RetrievalService] converted score = " + score);
            System.out.println("[RetrievalService] threshold = " + similarityThreshold);

            if (score != null && score < similarityThreshold) {
                System.out.println("[RetrievalService] Skip: score below threshold.");
                continue;
            }

            RetrievedChunk chunk = new RetrievedChunk();

            chunk.setChunkId(rawChunk.getChunkId());
            chunk.setDocumentId(rawChunk.getDocumentId());
            chunk.setFileName(rawChunk.getFileName());
            chunk.setPageNumber(rawChunk.getPageNumber());
            chunk.setSectionTitle(rawChunk.getSectionTitle());
            chunk.setContent(rawChunk.getContent());
            chunk.setScore(score);

            result.add(chunk);

            System.out.println("[RetrievalService] Accepted chunkId = " + rawChunk.getChunkId());

            if (result.size() >= limit) {
                break;
            }
        }

        System.out.println("[RetrievalService] final accepted chunks = " + result.size());

        return result;
    }

    private void validateUserId(Long userId) {
        if (userId == null) {
            throw new BusinessException(
                    "UNAUTHORIZED",
                    "Current user is required."
            );
        }
    }

    private void validateCourseId(Long courseId) {
        if (courseId == null) {
            throw new BusinessException(
                    "INVALID_COURSE_ID",
                    "Course id is required."
            );
        }
    }

    private int normalizeTopK(Integer topK) {
        if (topK == null) {
            return defaultTopK == null ? 8 : defaultTopK;
        }

        if (topK < 1) {
            return 1;
        }

        return Math.min(topK, 20);
    }

    private String normalizeRetrievalQuery(
            String query,
            String fallback
    ) {
        if (query == null || query.isBlank()) {
            return fallback;
        }

        return query.trim();
    }

    @Override
    public List<StudyToolDtos.DocumentSearchToolResult> searchFromVectorStore(
            Long userId,
            Long courseId,
            String query,
            int topK,
            Long documentId
    ) {
        System.out.println("[RetrievalService] Start tool-level document search.");
        System.out.println("[RetrievalService] userId = " + userId);
        System.out.println("[RetrievalService] courseId = " + courseId);
        System.out.println("[RetrievalService] documentId = " + documentId);
        System.out.println("[RetrievalService] query = " + query);
        System.out.println("[RetrievalService] topK = " + topK);

        List<RetrievedChunk> chunks;

        if (documentId == null) {
            chunks = retrieveCourseChunks(
                    userId,
                    courseId,
                    topK,
                    query
            );
        } else {
            chunks = retrieveDocumentChunks(
                    userId,
                    courseId,
                    documentId,
                    topK,
                    query
            );
        }

        return chunks.stream()
                .map(this::toDocumentSearchToolResult)
                .toList();
    }

    private StudyToolDtos.DocumentSearchToolResult toDocumentSearchToolResult(RetrievedChunk chunk) {
        if (chunk == null) {
            return null;
        }

        return new StudyToolDtos.DocumentSearchToolResult(
                chunk.getChunkId(),
                chunk.getDocumentId(),
                chunk.getFileName(),
                chunk.getPageNumber(),
                chunk.getSectionTitle(),
                chunk.getContent(),
                chunk.getScore()
        );
    }
}