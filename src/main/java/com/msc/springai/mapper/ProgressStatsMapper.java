package com.msc.springai.mapper;

import com.msc.springai.dto.learning.response.RecentActivityResponse;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ProgressStatsMapper {

    @Select("""
            SELECT COUNT(*)
            FROM courses
            WHERE user_id = #{userId}
            """)
    Integer countCourses(@Param("userId") Long userId);

    @Select("""
            SELECT COUNT(*)
            FROM documents
            WHERE user_id = #{userId}
            """)
    Integer countDocuments(@Param("userId") Long userId);

    @Select("""
            SELECT COUNT(*)
            FROM documents
            WHERE user_id = #{userId}
              AND status = 'READY'
            """)
    Integer countReadyDocuments(@Param("userId") Long userId);

    @Select("""
            SELECT COUNT(*)
            FROM chat_messages
            WHERE user_id = #{userId}
              AND role = 'USER'
            """)
    Integer countQuestionsAsked(@Param("userId") Long userId);

    @Select("""
            SELECT COUNT(*)
            FROM summaries
            WHERE user_id = #{userId}
            """)
    Integer countSummaries(@Param("userId") Long userId);

    @Select("""
            SELECT COUNT(*)
            FROM quizzes
            WHERE user_id = #{userId}
            """)
    Integer countQuizzes(@Param("userId") Long userId);

    @Select("""
            SELECT COUNT(*)
            FROM flashcards
            WHERE user_id = #{userId}
            """)
    Integer countFlashcards(@Param("userId") Long userId);

    @Select("""
            SELECT COALESCE(ROUND(AVG(score), 2), 0)
            FROM quiz_attempts
            WHERE user_id = #{userId}
            """)
    Double averageQuizScore(@Param("userId") Long userId);

    @Select("""
            SELECT COUNT(*)
            FROM documents
            WHERE user_id = #{userId}
              AND course_id = #{courseId}
            """)
    Integer countCourseDocuments(
            @Param("userId") Long userId,
            @Param("courseId") Long courseId
    );

    @Select("""
            SELECT COUNT(*)
            FROM documents
            WHERE user_id = #{userId}
              AND course_id = #{courseId}
              AND status = 'READY'
            """)
    Integer countCourseReadyDocuments(
            @Param("userId") Long userId,
            @Param("courseId") Long courseId
    );

    @Select("""
            SELECT COUNT(*)
            FROM chat_messages
            WHERE user_id = #{userId}
              AND course_id = #{courseId}
            """)
    Integer countCourseChatMessages(
            @Param("userId") Long userId,
            @Param("courseId") Long courseId
    );

    @Select("""
            SELECT COUNT(*)
            FROM summaries
            WHERE user_id = #{userId}
              AND course_id = #{courseId}
            """)
    Integer countCourseSummaries(
            @Param("userId") Long userId,
            @Param("courseId") Long courseId
    );

    @Select("""
            SELECT COUNT(*)
            FROM quizzes
            WHERE user_id = #{userId}
              AND course_id = #{courseId}
            """)
    Integer countCourseQuizzes(
            @Param("userId") Long userId,
            @Param("courseId") Long courseId
    );

    @Select("""
            SELECT COUNT(*)
            FROM quiz_attempts
            WHERE user_id = #{userId}
              AND course_id = #{courseId}
            """)
    Integer countCourseQuizAttempts(
            @Param("userId") Long userId,
            @Param("courseId") Long courseId
    );

    @Select("""
            SELECT COALESCE(ROUND(AVG(score), 2), 0)
            FROM quiz_attempts
            WHERE user_id = #{userId}
              AND course_id = #{courseId}
            """)
    Double averageCourseQuizScore(
            @Param("userId") Long userId,
            @Param("courseId") Long courseId
    );

    @Select("""
            SELECT COUNT(*)
            FROM wrong_answers
            WHERE user_id = #{userId}
              AND course_id = #{courseId}
            """)
    Integer countCourseWrongAnswers(
            @Param("userId") Long userId,
            @Param("courseId") Long courseId
    );

    @Select("""
            SELECT COUNT(*)
            FROM wrong_answers
            WHERE user_id = #{userId}
              AND course_id = #{courseId}
              AND resolved = FALSE
            """)
    Integer countCourseUnresolvedWrongAnswers(
            @Param("userId") Long userId,
            @Param("courseId") Long courseId
    );

    @Select("""
            SELECT COUNT(*)
            FROM flashcards
            WHERE user_id = #{userId}
              AND course_id = #{courseId}
            """)
    Integer countCourseFlashcards(
            @Param("userId") Long userId,
            @Param("courseId") Long courseId
    );

    @Update("""
            UPDATE courses
            SET progress_score = #{progressScore},
                updated_at = NOW()
            WHERE id = #{courseId}
              AND user_id = #{userId}
            """)
    int updateCourseProgressScore(
            @Param("userId") Long userId,
            @Param("courseId") Long courseId,
            @Param("progressScore") Double progressScore
    );

    @Select("""
            SELECT
                event_type,
                target_type,
                target_id,
                topic,
                created_at
            FROM learning_history
            WHERE user_id = #{userId}
            ORDER BY created_at DESC, id DESC
            LIMIT #{limit}
            """)
    @Results({
            @Result(property = "eventType", column = "event_type"),
            @Result(property = "targetType", column = "target_type"),
            @Result(property = "targetId", column = "target_id"),
            @Result(property = "topic", column = "topic"),
            @Result(property = "createdAt", column = "created_at")
    })
    List<RecentActivityResponse> findRecentActivityByUser(
            @Param("userId") Long userId,
            @Param("limit") Integer limit
    );

    @Select("""
            SELECT
                event_type,
                target_type,
                target_id,
                topic,
                created_at
            FROM learning_history
            WHERE user_id = #{userId}
              AND course_id = #{courseId}
            ORDER BY created_at DESC, id DESC
            LIMIT #{limit}
            """)
    @Results({
            @Result(property = "eventType", column = "event_type"),
            @Result(property = "targetType", column = "target_type"),
            @Result(property = "targetId", column = "target_id"),
            @Result(property = "topic", column = "topic"),
            @Result(property = "createdAt", column = "created_at")
    })
    List<RecentActivityResponse> findRecentActivityByCourse(
            @Param("userId") Long userId,
            @Param("courseId") Long courseId,
            @Param("limit") Integer limit
    );
}