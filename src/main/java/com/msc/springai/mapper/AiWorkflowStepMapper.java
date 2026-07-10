package com.msc.springai.mapper;

import com.msc.springai.entity.AiWorkflowStep;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface AiWorkflowStepMapper {

    @Insert("""
        INSERT INTO ai_workflow_steps (
            workflow_run_id,
            step_name,
            status,
            started_at,
            completed_at,
            error_message
        )
        VALUES (
            #{workflowRunId},
            #{stepName},
            #{status},
            #{startedAt},
            #{completedAt},
            #{errorMessage}
        )
    """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AiWorkflowStep step);

    @Select("""
        SELECT
            id,
            workflow_run_id AS workflowRunId,
            step_name AS stepName,
            status,
            started_at AS startedAt,
            completed_at AS completedAt,
            error_message AS errorMessage
        FROM ai_workflow_steps
        WHERE id = #{id}
    """)
    AiWorkflowStep findById(Long id);

    @Select("""
        SELECT
            id,
            workflow_run_id AS workflowRunId,
            step_name AS stepName,
            status,
            started_at AS startedAt,
            completed_at AS completedAt,
            error_message AS errorMessage
        FROM ai_workflow_steps
        WHERE workflow_run_id = #{workflowRunId}
        ORDER BY started_at ASC, id ASC
    """)
    List<AiWorkflowStep> findByWorkflowRunId(Long workflowRunId);

    @Update("""
        UPDATE ai_workflow_steps
        SET
            status = 'SUCCESS',
            error_message = NULL,
            completed_at = NOW()
        WHERE id = #{id}
    """)
    int markSuccess(Long id);

    @Update("""
        UPDATE ai_workflow_steps
        SET
            status = 'FAILED',
            error_message = #{errorMessage},
            completed_at = NOW()
        WHERE id = #{id}
    """)
    int markFailed(@Param("id") Long id,
                   @Param("errorMessage") String errorMessage);
}