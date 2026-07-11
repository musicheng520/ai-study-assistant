package com.msc.springai.mapper;

import com.msc.springai.entity.AiRequestLog;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;

@Mapper
public interface AiRequestLogMapper {

    @Insert("""
            INSERT INTO ai_request_logs (
                user_id,
                course_id,
                workflow_type,
                model_name,
                prompt_tokens,
                completion_tokens,
                total_tokens,
                latency_ms,
                cache_hit,
                retrieved_chunk_count,
                error_type,
                error_message,
                created_at
            )
            VALUES (
                #{userId},
                #{courseId},
                #{workflowType},
                #{modelName},
                #{promptTokens},
                #{completionTokens},
                #{totalTokens},
                #{latencyMs},
                #{cacheHit},
                #{retrievedChunkCount},
                #{errorType},
                #{errorMessage},
                #{createdAt}
            )
            """)
    @Options(
            useGeneratedKeys = true,
            keyProperty = "id"
    )
    void insert(AiRequestLog requestLog);
}