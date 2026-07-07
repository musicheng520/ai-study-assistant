package com.msc.springai.mapper;

import com.msc.springai.entity.DocumentProcessingJob;
import org.apache.ibatis.annotations.*;

@Mapper
public interface DocumentProcessingJobMapper {

    @Insert("""
            INSERT INTO document_processing_jobs (
                user_id, course_id, document_id, status, step, retry_count
            )
            VALUES (
                #{userId}, #{courseId}, #{documentId}, #{status}, #{step}, #{retryCount}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(DocumentProcessingJob job);

    @Select("""
            SELECT id, user_id, course_id, document_id, status, step,
                   error_message, retry_count, started_at, completed_at, created_at
            FROM document_processing_jobs
            WHERE id = #{id}
            """)
    DocumentProcessingJob findById(Long id);

    @Update("""
    UPDATE document_processing_jobs
    SET status = 'RUNNING',
        step = #{step},
        error_message = NULL,
        started_at = IFNULL(started_at, NOW())
    WHERE id = #{jobId}
      AND user_id = #{userId}
""")
    int markRunning(
            @Param("jobId") Long jobId,
            @Param("userId") Long userId,
            @Param("step") String step
    );

    @Update("""
    UPDATE document_processing_jobs
    SET step = #{step}
    WHERE id = #{jobId}
      AND user_id = #{userId}
""")
    int updateStep(
            @Param("jobId") Long jobId,
            @Param("userId") Long userId,
            @Param("step") String step
    );

    @Update("""
    UPDATE document_processing_jobs
    SET status = 'SUCCESS',
        step = 'DONE',
        error_message = NULL,
        completed_at = NOW()
    WHERE id = #{jobId}
      AND user_id = #{userId}
""")
    int markSuccess(
            @Param("jobId") Long jobId,
            @Param("userId") Long userId
    );

    @Update("""
    UPDATE document_processing_jobs
    SET status = 'FAILED',
        error_message = #{errorMessage},
        completed_at = NOW()
    WHERE id = #{jobId}
      AND user_id = #{userId}
""")
    int markFailed(
            @Param("jobId") Long jobId,
            @Param("userId") Long userId,
            @Param("errorMessage") String errorMessage
    );
}