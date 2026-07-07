package com.msc.springai.mapper;

import com.msc.springai.entity.ChatMessage;
import org.apache.ibatis.annotations.*;

@Mapper
public interface ChatMessageMapper {

    @Insert("""
        INSERT INTO chat_messages (
            session_id,
            user_id,
            course_id,
            role,
            content,
            workflow_type,
            no_answer,
            model_name
        ) VALUES (
            #{sessionId},
            #{userId},
            #{courseId},
            #{role},
            #{content},
            #{workflowType},
            #{noAnswer},
            #{modelName}
        )
    """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ChatMessage chatMessage);

    @Select("""
        SELECT
            id,
            session_id AS sessionId,
            user_id AS userId,
            course_id AS courseId,
            role,
            content,
            workflow_type AS workflowType,
            no_answer AS noAnswer,
            model_name AS modelName,
            created_at AS createdAt
        FROM chat_messages
        WHERE id = #{messageId}
          AND user_id = #{userId}
    """)
    ChatMessage findByIdAndUserId(
            @Param("messageId") Long messageId,
            @Param("userId") Long userId
    );
}