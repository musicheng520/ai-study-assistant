package com.msc.springai.service;

import com.msc.springai.dto.workflow.tool.StudyToolDtos.DocumentSearchToolResult;
import com.msc.springai.exception.BusinessException;
import com.msc.springai.mapper.StudyAgentToolMapper;
import com.msc.springai.service.tool.DocumentSearchToolGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StudyDocumentSearchToolService {

    private static final int DEFAULT_TOP_K = 5;
    private static final int MAX_TOP_K = 10;

    private final StudyAgentToolMapper studyAgentToolMapper;
    private final DocumentSearchToolGateway documentSearchToolGateway;

    public List<DocumentSearchToolResult> searchDocumentTool(Long userId,
                                                             Long courseId,
                                                             String query,
                                                             Integer topK,
                                                             Long documentId) {
        validateBasicInput(userId, courseId, query);

        ensureCourseAccess(userId, courseId);

        ensureReadyDocumentsExist(userId, courseId);

        if (documentId != null) {
            ensureDocumentAccess(userId, courseId, documentId);
        }

        int safeTopK = normalizeTopK(topK);

        return documentSearchToolGateway.searchFromVectorStore(
                userId,
                courseId,
                query.trim(),
                safeTopK,
                documentId
        );
    }

    private void validateBasicInput(Long userId, Long courseId, String query) {
        if (userId == null) {
            throw new BusinessException("Current user is required");
        }

        if (courseId == null) {
            throw new BusinessException("Course id is required");
        }

        if (query == null || query.isBlank()) {
            throw new BusinessException("Search query is required");
        }
    }

    private void ensureCourseAccess(Long userId, Long courseId) {
        int count = studyAgentToolMapper.countCourseOwnership(userId, courseId);

        if (count == 0) {
            throw new BusinessException("Course not found or access denied");
        }
    }

    private void ensureReadyDocumentsExist(Long userId, Long courseId) {
        int count = studyAgentToolMapper.countReadyDocumentsInCourse(userId, courseId);

        if (count == 0) {
            throw new BusinessException("No READY documents found in this course");
        }
    }

    private void ensureDocumentAccess(Long userId, Long courseId, Long documentId) {
        int count = studyAgentToolMapper.countReadyDocumentAccess(
                userId,
                courseId,
                documentId
        );

        if (count == 0) {
            throw new BusinessException("Document not found, not READY, or access denied");
        }
    }

    private int normalizeTopK(Integer topK) {
        if (topK == null || topK <= 0) {
            return DEFAULT_TOP_K;
        }

        return Math.min(topK, MAX_TOP_K);
    }
}