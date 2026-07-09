package com.msc.springai.mapper;

import com.msc.springai.dto.learning.projection.DocumentReviewCandidate;
import com.msc.springai.dto.learning.projection.LowScoreQuizCandidate;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;

@Mapper
public interface ProgressRecommendationMapper {

    @Select("""
            SELECT COUNT(*)
            FROM learning_history
            WHERE user_id = #{userId}
              AND course_id = #{courseId}
              AND event_type = 'REVIEW'
              AND target_type = 'TOPIC'
              AND topic = #{topic}
              AND created_at >= #{since}
            """)
    Integer countTopicReviewsSince(
            @Param("userId") Long userId,
            @Param("courseId") Long courseId,
            @Param("topic") String topic,
            @Param("since") LocalDateTime since
    );

    @Select("""
            SELECT
                qa.quiz_id AS quiz_id,
                q.title AS quiz_title,
                qa.score AS score,
                MIN(qq.topic) AS topic,
                qa.submitted_at AS submitted_at
            FROM quiz_attempts qa
            JOIN quizzes q
              ON q.id = qa.quiz_id
            LEFT JOIN quiz_questions qq
              ON qq.quiz_id = q.id
            WHERE qa.user_id = #{userId}
              AND qa.course_id = #{courseId}
              AND qa.score < #{threshold}
            GROUP BY qa.id, qa.quiz_id, q.title, qa.score, qa.submitted_at
            ORDER BY qa.submitted_at DESC, qa.id DESC
            LIMIT 1
            """)
    @Results({
            @Result(property = "quizId", column = "quiz_id"),
            @Result(property = "quizTitle", column = "quiz_title"),
            @Result(property = "score", column = "score"),
            @Result(property = "topic", column = "topic"),
            @Result(property = "submittedAt", column = "submitted_at")
    })
    LowScoreQuizCandidate findLatestLowScoreQuiz(
            @Param("userId") Long userId,
            @Param("courseId") Long courseId,
            @Param("threshold") Double threshold
    );

    @Select("""
            SELECT
                d.id AS document_id,
                d.original_file_name AS file_name,
                d.created_at AS created_at
            FROM documents d
            WHERE d.user_id = #{userId}
              AND d.course_id = #{courseId}
              AND d.status = 'READY'
              AND NOT EXISTS (
                  SELECT 1
                  FROM quizzes q
                  JOIN quiz_attempts qa
                    ON qa.quiz_id = q.id
                   AND qa.user_id = #{userId}
                  WHERE q.user_id = #{userId}
                    AND q.course_id = #{courseId}
                    AND q.document_id = d.id
              )
            ORDER BY d.created_at DESC, d.id DESC
            LIMIT 1
            """)
    @Results({
            @Result(property = "documentId", column = "document_id"),
            @Result(property = "fileName", column = "file_name"),
            @Result(property = "createdAt", column = "created_at")
    })
    DocumentReviewCandidate findRecentReadyDocumentWithoutQuizAttempt(
            @Param("userId") Long userId,
            @Param("courseId") Long courseId
    );
}