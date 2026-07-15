package com.msc.springai.service;

import com.msc.springai.dto.rag.history.ChatHistoryDtos.ChatCitationHistoryResponse;
import com.msc.springai.dto.rag.history.ChatHistoryDtos.ChatCitationHistoryRow;
import com.msc.springai.dto.rag.history.ChatHistoryDtos.ChatMessageHistoryResponse;
import com.msc.springai.dto.rag.history.ChatHistoryDtos.ChatMessageHistoryRow;
import com.msc.springai.dto.rag.history.ChatHistoryDtos.ChatSessionDetailResponse;
import com.msc.springai.dto.rag.history.ChatHistoryDtos.ChatSessionDetailRow;
import com.msc.springai.dto.rag.history.ChatHistoryDtos.ChatSessionListItemResponse;
import com.msc.springai.exception.BusinessException;
import com.msc.springai.mapper.ChatHistoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ChatHistoryService {

    private static final int DEFAULT_LIMIT = 30;

    private static final int MAX_LIMIT = 100;

    private final ChatHistoryMapper chatHistoryMapper;

    public List<ChatSessionListItemResponse> getCourseChatSessions(
            Long currentUserId,
            Long courseId,
            Integer limit,
            Integer offset
    ) {
        validateCurrentUser(currentUserId);
        validateCourseId(courseId);

        ensureCourseAccess(
                currentUserId,
                courseId
        );

        return chatHistoryMapper.findSessionsByCourseId(
                currentUserId,
                courseId,
                normalizeLimit(limit),
                normalizeOffset(offset)
        );
    }

    public ChatSessionDetailResponse getChatSessionDetail(
            Long currentUserId,
            Long sessionId
    ) {
        validateCurrentUser(currentUserId);
        validateSessionId(sessionId);

        ChatSessionDetailRow session =
                chatHistoryMapper.findSessionDetailHeader(
                        currentUserId,
                        sessionId
                );

        if (session == null) {
            throw new BusinessException(
                    "CHAT_SESSION_NOT_FOUND",
                    "Chat session not found or access denied."
            );
        }

        List<ChatMessageHistoryRow> messageRows =
                chatHistoryMapper.findMessagesBySessionId(
                        currentUserId,
                        sessionId
                );

        List<ChatCitationHistoryRow> citationRows =
                chatHistoryMapper.findCitationsBySessionId(
                        currentUserId,
                        sessionId
                );

        Map<Long, List<ChatCitationHistoryResponse>> citationsByMessageId =
                groupCitationsByMessageId(citationRows);

        List<ChatMessageHistoryResponse> messages =
                messageRows.stream()
                        .map(row -> toMessageResponse(
                                row,
                                citationsByMessageId.getOrDefault(
                                        row.getId(),
                                        List.of()
                                )
                        ))
                        .toList();

        return new ChatSessionDetailResponse(
                session.getId(),
                session.getCourseId(),
                session.getTitle(),
                session.getScopeType(),
                session.getDocumentId(),
                session.getCreatedAt(),
                session.getUpdatedAt(),
                messages
        );
    }

    private Map<Long, List<ChatCitationHistoryResponse>> groupCitationsByMessageId(
            List<ChatCitationHistoryRow> rows
    ) {
        Map<Long, List<ChatCitationHistoryResponse>> result =
                new LinkedHashMap<>();

        Map<Long, Integer> indexByMessageId =
                new LinkedHashMap<>();

        for (ChatCitationHistoryRow row : rows) {
            if (row == null || row.getMessageId() == null) {
                continue;
            }

            int nextIndex =
                    indexByMessageId.getOrDefault(
                            row.getMessageId(),
                            0
                    ) + 1;

            indexByMessageId.put(
                    row.getMessageId(),
                    nextIndex
            );

            ChatCitationHistoryResponse citation =
                    new ChatCitationHistoryResponse(
                            nextIndex,
                            row.getDocumentId(),
                            row.getChunkId(),
                            row.getFileName(),
                            row.getPageNumber(),
                            row.getSectionTitle(),
                            row.getSnippet(),
                            row.getCreatedAt()
                    );

            result.computeIfAbsent(
                    row.getMessageId(),
                    ignored -> new ArrayList<>()
            ).add(citation);
        }

        return result;
    }

    private ChatMessageHistoryResponse toMessageResponse(
            ChatMessageHistoryRow row,
            List<ChatCitationHistoryResponse> citations
    ) {
        return new ChatMessageHistoryResponse(
                row.getId(),
                row.getRole(),
                row.getContent(),
                row.getWorkflowType(),
                Boolean.TRUE.equals(row.getNoAnswer()),
                row.getModelName(),
                row.getCreatedAt(),
                citations
        );
    }

    private void ensureCourseAccess(
            Long userId,
            Long courseId
    ) {
        int count =
                chatHistoryMapper.countCourseOwnership(
                        userId,
                        courseId
                );

        if (count == 0) {
            throw new BusinessException(
                    "COURSE_NOT_FOUND",
                    "Course not found or access denied."
            );
        }
    }

    private void validateCurrentUser(
            Long currentUserId
    ) {
        if (currentUserId == null) {
            throw new BusinessException(
                    "UNAUTHORIZED",
                    "Current user is required."
            );
        }
    }

    private void validateCourseId(
            Long courseId
    ) {
        if (courseId == null) {
            throw new BusinessException(
                    "INVALID_COURSE_ID",
                    "Course id is required."
            );
        }
    }

    private void validateSessionId(
            Long sessionId
    ) {
        if (sessionId == null) {
            throw new BusinessException(
                    "INVALID_CHAT_SESSION_ID",
                    "Chat session id is required."
            );
        }
    }

    private int normalizeLimit(
            Integer limit
    ) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }

        if (limit < 1) {
            return 1;
        }

        return Math.min(
                limit,
                MAX_LIMIT
        );
    }

    private int normalizeOffset(
            Integer offset
    ) {
        if (offset == null || offset < 0) {
            return 0;
        }

        return offset;
    }
}