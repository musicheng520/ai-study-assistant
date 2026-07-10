package com.msc.springai.mapper;

import com.msc.springai.entity.AiWorkflowRun;
import org.apache.ibatis.annotations.*;

@Mapper
public interface AiWorkflowRunMapper {

    @Insert("""
        INSERT INTO ai_workflow_runs (
            user_id,
            course_id,
            workflow_type,
            status,
            input_json,
            output_json,
            error_message,
            started_at,
            completed_at
        )
        VALUES (
            #{userId},
            #{courseId},
            #{workflowType},
            #{status},
            #{inputJson},
            #{outputJson},
            #{errorMessage},
            #{startedAt},
            #{completedAt}
        )
    """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AiWorkflowRun run);

    @Select("""
        SELECT
            id,
            user_id AS userId,
            course_id AS courseId,
            workflow_type AS workflowType,
            status,
            input_json AS inputJson,
            output_json AS outputJson,
            error_message AS errorMessage,
            started_at AS startedAt,
            completed_at AS completedAt
        FROM ai_workflow_runs
        WHERE id = #{id}
    """)
    AiWorkflowRun findById(Long id);

    @Select("""
        SELECT
            id,
            user_id AS userId,
            course_id AS courseId,
            workflow_type AS workflowType,
            status,
            input_json AS inputJson,
            output_json AS outputJson,
            error_message AS errorMessage,
            started_at AS startedAt,
            completed_at AS completedAt
        FROM ai_workflow_runs
        WHERE id = #{id}
          AND user_id = #{userId}
    """)
    AiWorkflowRun findByIdAndUserId(@Param("id") Long id,
                                    @Param("userId") Long userId);

    @Update("""
        UPDATE ai_workflow_runs
        SET
            status = 'SUCCESS',
            output_json = #{outputJson},
            error_message = NULL,
            completed_at = NOW()
        WHERE id = #{id}
    """)
    int markSuccess(@Param("id") Long id,
                    @Param("outputJson") String outputJson);

    @Update("""
        UPDATE ai_workflow_runs
        SET
            status = 'FAILED',
            error_message = #{errorMessage},
            completed_at = NOW()
        WHERE id = #{id}
    """)
    int markFailed(@Param("id") Long id,
                   @Param("errorMessage") String errorMessage);
}