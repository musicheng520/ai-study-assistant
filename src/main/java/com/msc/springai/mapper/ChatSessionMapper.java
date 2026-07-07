package com.msc.springai.mapper;

import com.msc.springai.entity.ChatSession;
import org.apache.ibatis.annotations.*;

@Mapper
public interface ChatSessionMapper {

    @Insert("""
        INSERT INTO chat_sessions (
            user_id,
            course_id,
            title,
            scope_type,
            document_id
        ) VALUES (
            #{userId},
            #{courseId},
            #{title},
            #{scopeType},
            #{documentId}
        )
    """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ChatSession chatSession);

    @Select("""
        SELECT
            id,
            user_id AS userId,
            course_id AS courseId,
            title,
            scope_type AS scopeType,
            document_id AS documentId,
            created_at AS createdAt,
            updated_at AS updatedAt
        FROM chat_sessions
        WHERE id = #{sessionId}
          AND user_id = #{userId}
    """)
    ChatSession findByIdAndUserId(
            @Param("sessionId") Long sessionId,
            @Param("userId") Long userId
    );

    @Update("""
        UPDATE chat_sessions
        SET updated_at = NOW()
        WHERE id = #{sessionId}
          AND user_id = #{userId}
    """)
    int touch(
            @Param("sessionId") Long sessionId,
            @Param("userId") Long userId
    );
}