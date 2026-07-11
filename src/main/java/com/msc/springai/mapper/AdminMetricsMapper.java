package com.msc.springai.mapper;

import com.msc.springai.dto.admin.AdminMetricsDtos.AiMetricsSummaryRow;
import com.msc.springai.dto.admin.AdminMetricsDtos.AiRequestLogResponse;
import com.msc.springai.dto.admin.AdminMetricsDtos.CacheMetricsSummaryRow;
import com.msc.springai.dto.admin.AdminMetricsDtos.RecentAiFailureItem;
import com.msc.springai.dto.admin.AdminMetricsDtos.WorkflowRunLogResponse;
import com.msc.springai.dto.admin.AdminMetricsDtos.WorkflowUsageItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AdminMetricsMapper {

    /*
     * Admin 权限检查。
     *
     * 不依赖前端是否隐藏 Admin 页面。
     * 后端必须自己判断 role。
     */
    @Select("""
            SELECT COUNT(*)
            FROM users
            WHERE id = #{userId}
              AND role = 'ADMIN'
              AND status = 'ACTIVE'
            """)
    int countActiveAdmin(
            @Param("userId") Long userId
    );

    /*
     * AI 总览指标。
     */
    @Select("""
            SELECT
                COUNT(*) AS totalRequests,

                SUM(
                    CASE
                        WHEN error_type IS NULL THEN 1
                        ELSE 0
                    END
                ) AS successCount,

                SUM(
                    CASE
                        WHEN error_type IS NOT NULL THEN 1
                        ELSE 0
                    END
                ) AS failedCount,

                COALESCE(SUM(total_tokens), 0) AS totalTokens,

                COALESCE(AVG(latency_ms), 0) AS averageLatencyMs,

                COALESCE(MAX(latency_ms), 0) AS maxLatencyMs,

                COALESCE(AVG(retrieved_chunk_count), 0)
                    AS averageRetrievedChunkCount

            FROM ai_request_logs
            WHERE created_at >= DATE_SUB(NOW(), INTERVAL #{days} DAY)
            """)
    AiMetricsSummaryRow findAiMetricsSummary(
            @Param("days") Integer days
    );

    /*
     * 按 workflow_type 统计调用量。
     */
    @Select("""
            SELECT
                workflow_type AS workflowType,

                COUNT(*) AS requestCount,

                SUM(
                    CASE
                        WHEN error_type IS NOT NULL THEN 1
                        ELSE 0
                    END
                ) AS failedCount,

                COALESCE(SUM(total_tokens), 0) AS totalTokens,

                COALESCE(AVG(latency_ms), 0) AS averageLatencyMs

            FROM ai_request_logs
            WHERE created_at >= DATE_SUB(NOW(), INTERVAL #{days} DAY)
            GROUP BY workflow_type
            ORDER BY requestCount DESC
            LIMIT #{limit}
            """)
    List<WorkflowUsageItem> findWorkflowUsage(
            @Param("days") Integer days,
            @Param("limit") Integer limit
    );

    /*
     * 最近 AI 失败日志。
     */
    @Select("""
            SELECT
                id,
                user_id AS userId,
                course_id AS courseId,
                workflow_type AS workflowType,
                model_name AS modelName,
                error_type AS errorType,
                error_message AS errorMessage,
                created_at AS createdAt

            FROM ai_request_logs
            WHERE error_type IS NOT NULL
            ORDER BY created_at DESC, id DESC
            LIMIT #{limit}
            """)
    List<RecentAiFailureItem> findRecentFailures(
            @Param("limit") Integer limit
    );

    /*
     * RAG cache 指标。
     *
     * 只统计 workflow_type = RAG_QA。
     */
    @Select("""
            SELECT
                COUNT(*) AS ragRequestCount,

                SUM(
                    CASE
                        WHEN cache_hit = 1 THEN 1
                        ELSE 0
                    END
                ) AS cacheHitCount,

                SUM(
                    CASE
                        WHEN cache_hit = 0 THEN 1
                        ELSE 0
                    END
                ) AS cacheMissCount,

                COALESCE(
                    AVG(
                        CASE
                            WHEN cache_hit = 1 THEN latency_ms
                            ELSE NULL
                        END
                    ),
                    0
                ) AS averageCacheHitLatencyMs,

                COALESCE(
                    AVG(
                        CASE
                            WHEN cache_hit = 0 THEN latency_ms
                            ELSE NULL
                        END
                    ),
                    0
                ) AS averageCacheMissLatencyMs,

                COALESCE(AVG(retrieved_chunk_count), 0)
                    AS averageRetrievedChunkCount

            FROM ai_request_logs
            WHERE workflow_type = 'RAG_QA'
              AND created_at >= DATE_SUB(NOW(), INTERVAL #{days} DAY)
            """)
    CacheMetricsSummaryRow findCacheMetricsSummary(
            @Param("days") Integer days
    );

    /*
     * AI request log 表格。
     *
     * workflowType 可以为空。
     * onlyFailures = true 时只看失败。
     */
    @Select("""
            SELECT
                id,
                user_id AS userId,
                course_id AS courseId,
                workflow_type AS workflowType,
                model_name AS modelName,
                prompt_tokens AS promptTokens,
                completion_tokens AS completionTokens,
                total_tokens AS totalTokens,
                latency_ms AS latencyMs,
                cache_hit AS cacheHit,
                retrieved_chunk_count AS retrievedChunkCount,
                error_type AS errorType,
                error_message AS errorMessage,
                created_at AS createdAt

            FROM ai_request_logs
            WHERE (
                    #{workflowType} IS NULL
                    OR workflow_type = #{workflowType}
                  )
              AND (
                    #{onlyFailures} = FALSE
                    OR error_type IS NOT NULL
                  )
            ORDER BY created_at DESC, id DESC
            LIMIT #{limit}
            OFFSET #{offset}
            """)
    List<AiRequestLogResponse> findAiRequestLogs(
            @Param("workflowType") String workflowType,
            @Param("onlyFailures") Boolean onlyFailures,
            @Param("limit") Integer limit,
            @Param("offset") Integer offset
    );

    /*
     * Workflow runs 日志表。
     */
    @Select("""
            SELECT
                r.id,
                r.user_id AS userId,
                r.course_id AS courseId,
                r.workflow_type AS workflowType,
                r.status,
                r.error_message AS errorMessage,
                r.started_at AS startedAt,
                r.completed_at AS completedAt,

                CASE
                    WHEN r.completed_at IS NULL THEN NULL
                    ELSE TIMESTAMPDIFF(
                            MICROSECOND,
                            r.started_at,
                            r.completed_at
                         ) / 1000
                END AS durationMs,

                (
                    SELECT COUNT(*)
                    FROM ai_workflow_steps s
                    WHERE s.workflow_run_id = r.id
                ) AS stepCount

            FROM ai_workflow_runs r
            WHERE (
                    #{status} IS NULL
                    OR r.status = #{status}
                  )
              AND (
                    #{workflowType} IS NULL
                    OR r.workflow_type = #{workflowType}
                  )
            ORDER BY r.started_at DESC, r.id DESC
            LIMIT #{limit}
            OFFSET #{offset}
            """)
    List<WorkflowRunLogResponse> findWorkflowRuns(
            @Param("status") String status,
            @Param("workflowType") String workflowType,
            @Param("limit") Integer limit,
            @Param("offset") Integer offset
    );
}