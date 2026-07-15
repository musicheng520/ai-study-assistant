package com.msc.springai.mapper;

import com.msc.springai.dto.rag.history.ChatHistoryDtos.ChatCitationHistoryRow;
import com.msc.springai.dto.rag.history.ChatHistoryDtos.ChatMessageHistoryRow;
import com.msc.springai.dto.rag.history.ChatHistoryDtos.ChatSessionDetailRow;
import com.msc.springai.dto.rag.history.ChatHistoryDtos.ChatSessionListItemResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ChatHistoryMapper {

    @Select("""
            SELECT COUNT(*)
            FROM courses
            WHERE id = #{courseId}
              AND user_id = #{userId}
            """)
    int countCourseOwnership(
            @Param("userId") Long userId,
            @Param("courseId") Long courseId
    );

    @Select("""
            SELECT
                cs.id,
                cs.course_id AS courseId,
                cs.title,
                cs.scope_type AS scopeType,
                cs.document_id AS documentId,
                (
                    SELECT COUNT(*)
                    FROM chat_messages cm
                    WHERE cm.session_id = cs.id
                      AND cm.user_id = #{userId}
                ) AS messageCount,
                (
                    SELECT LEFT(cm2.content, 160)
                    FROM chat_messages cm2
                    WHERE cm2.session_id = cs.id
                      AND cm2.user_id = #{userId}
                    ORDER BY cm2.created_at DESC, cm2.id DESC
                    LIMIT 1
                ) AS lastMessagePreview,
                cs.created_at AS createdAt,
                cs.updated_at AS updatedAt
            FROM chat_sessions cs
            INNER JOIN courses c
                    ON c.id = cs.course_id
                   AND c.user_id = #{userId}
            WHERE cs.user_id = #{userId}
              AND cs.course_id = #{courseId}
            ORDER BY cs.updated_at DESC, cs.id DESC
            LIMIT #{limit}
            OFFSET #{offset}
            """)
    List<ChatSessionListItemResponse> findSessionsByCourseId(
            @Param("userId") Long userId,
            @Param("courseId") Long courseId,
            @Param("limit") Integer limit,
            @Param("offset") Integer offset
    );

    @Select("""
            SELECT
                cs.id,
                cs.course_id AS courseId,
                cs.title,
                cs.scope_type AS scopeType,
                cs.document_id AS documentId,
                cs.created_at AS createdAt,
                cs.updated_at AS updatedAt
            FROM chat_sessions cs
            INNER JOIN courses c
                    ON c.id = cs.course_id
                   AND c.user_id = #{userId}
            WHERE cs.id = #{sessionId}
              AND cs.user_id = #{userId}
            """)
    ChatSessionDetailRow findSessionDetailHeader(
            @Param("userId") Long userId,
            @Param("sessionId") Long sessionId
    );

    @Select("""
            SELECT
                id,
                role,
                content,
                workflow_type AS workflowType,
                no_answer AS noAnswer,
                model_name AS modelName,
                created_at AS createdAt
            FROM chat_messages
            WHERE session_id = #{sessionId}
              AND user_id = #{userId}
            ORDER BY created_at ASC, id ASC
            """)
    List<ChatMessageHistoryRow> findMessagesBySessionId(
            @Param("userId") Long userId,
            @Param("sessionId") Long sessionId
    );

    @Select("""
            SELECT
                c.message_id AS messageId,
                c.document_id AS documentId,
                c.chunk_id AS chunkId,
                c.file_name AS fileName,
                c.page_number AS pageNumber,
                c.section_title AS sectionTitle,
                c.snippet,
                c.created_at AS createdAt
            FROM chat_message_citations c
            INNER JOIN chat_messages m
                    ON m.id = c.message_id
                   AND m.session_id = #{sessionId}
                   AND m.user_id = #{userId}
            INNER JOIN chat_sessions s
                    ON s.id = m.session_id
                   AND s.user_id = #{userId}
            ORDER BY c.message_id ASC, c.id ASC
            """)
    List<ChatCitationHistoryRow> findCitationsBySessionId(
            @Param("userId") Long userId,
            @Param("sessionId") Long sessionId
    );
}