package com.msc.springai.mapper;

import com.msc.springai.dto.dashboard.CourseOverviewDtos.CourseHeaderRow;
import com.msc.springai.dto.dashboard.CourseOverviewDtos.CourseOverviewStatsRow;
import com.msc.springai.dto.dashboard.CourseOverviewDtos.RecentActivityRow;
import com.msc.springai.dto.dashboard.CourseOverviewDtos.WeakTopicItemResponse;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface CourseOverviewMapper {

    @Select("""
            SELECT
                id AS courseId,
                name AS courseName,
                code AS courseCode,
                color AS courseColor,
                progress_score AS storedProgressScore
            FROM courses
            WHERE id = #{courseId}
              AND user_id = #{userId}
            """)
    CourseHeaderRow findCourseHeader(
            @Param("userId") Long userId,
            @Param("courseId") Long courseId
    );

    @Select("""
            SELECT
                (
                    SELECT COUNT(*)
                    FROM documents d
                    WHERE d.user_id = #{userId}
                      AND d.course_id = #{courseId}
                ) AS documentCount,

                (
                    SELECT COUNT(*)
                    FROM documents d
                    WHERE d.user_id = #{userId}
                      AND d.course_id = #{courseId}
                      AND d.status = 'READY'
                ) AS readyDocumentCount,

                (
                    SELECT COUNT(*)
                    FROM documents d
                    WHERE d.user_id = #{userId}
                      AND d.course_id = #{courseId}
                      AND d.status = 'PROCESSING'
                ) AS processingDocumentCount,

                (
                    SELECT COUNT(*)
                    FROM documents d
                    WHERE d.user_id = #{userId}
                      AND d.course_id = #{courseId}
                      AND d.status = 'FAILED'
                ) AS failedDocumentCount,

                (
                    SELECT COUNT(*)
                    FROM chat_messages m
                    WHERE m.user_id = #{userId}
                      AND m.course_id = #{courseId}
                      AND m.role = 'ASSISTANT'
                ) AS chatMessageCount,

                (
                    SELECT COUNT(*)
                    FROM summaries s
                    WHERE s.user_id = #{userId}
                      AND s.course_id = #{courseId}
                ) AS summaryCount,

                (
                    SELECT COUNT(*)
                    FROM quizzes q
                    WHERE q.user_id = #{userId}
                      AND q.course_id = #{courseId}
                ) AS quizCount,

                (
                    SELECT COUNT(*)
                    FROM quiz_attempts qa
                    WHERE qa.user_id = #{userId}
                      AND qa.course_id = #{courseId}
                ) AS quizAttemptCount,

                (
                    SELECT COALESCE(AVG(qa.score), 0)
                    FROM quiz_attempts qa
                    WHERE qa.user_id = #{userId}
                      AND qa.course_id = #{courseId}
                ) AS averageQuizScore,

                (
                    SELECT COUNT(*)
                    FROM wrong_answers wa
                    WHERE wa.user_id = #{userId}
                      AND wa.course_id = #{courseId}
                ) AS wrongAnswerCount,

                (
                    SELECT COUNT(*)
                    FROM wrong_answers wa
                    WHERE wa.user_id = #{userId}
                      AND wa.course_id = #{courseId}
                      AND wa.resolved = 0
                ) AS unresolvedWrongAnswerCount,

                (
                    SELECT COUNT(*)
                    FROM flashcards f
                    WHERE f.user_id = #{userId}
                      AND f.course_id = #{courseId}
                ) AS flashcardCount,

                0 AS noteCount,

                (
                    SELECT COUNT(*)
                    FROM study_tasks st
                    WHERE st.user_id = #{userId}
                      AND st.course_id = #{courseId}
                ) AS taskCount,

                (
                    SELECT COUNT(*)
                    FROM study_tasks st
                    WHERE st.user_id = #{userId}
                      AND st.course_id = #{courseId}
                      AND st.status = 'DONE'
                ) AS completedTaskCount,

                (
                    SELECT COUNT(*)
                    FROM revision_packs rp
                    WHERE rp.user_id = #{userId}
                      AND rp.course_id = #{courseId}
                ) AS revisionPackCount,

                (
                    SELECT COUNT(*)
                    FROM assignment_analyses aa
                    WHERE aa.user_id = #{userId}
                      AND aa.course_id = #{courseId}
                ) AS assignmentAnalysisCount,

                (
                    SELECT COUNT(*)
                    FROM rubric_analyses ra
                    WHERE ra.user_id = #{userId}
                      AND ra.course_id = #{courseId}
                ) AS rubricAnalysisCount
            """)
    CourseOverviewStatsRow findCourseStats(
            @Param("userId") Long userId,
            @Param("courseId") Long courseId
    );

    @Select("""
            SELECT
                topic,
                COUNT(*) AS wrongCount,
                SUM(
                    CASE
                        WHEN resolved = 0 THEN 1
                        ELSE 0
                    END
                ) AS unresolvedCount,
                MAX(created_at) AS latestWrongAt
            FROM wrong_answers
            WHERE user_id = #{userId}
              AND course_id = #{courseId}
              AND topic IS NOT NULL
              AND topic <> ''
            GROUP BY topic
            ORDER BY
                unresolvedCount DESC,
                wrongCount DESC,
                latestWrongAt DESC
            LIMIT #{limit}
            """)
    List<WeakTopicItemResponse> findWeakTopics(
            @Param("userId") Long userId,
            @Param("courseId") Long courseId,
            @Param("limit") Integer limit
    );

    @Select("""
            SELECT
                id,
                event_type AS eventType,
                target_type AS targetType,
                target_id AS targetId,
                topic,
                created_at AS createdAt
            FROM learning_history
            WHERE user_id = #{userId}
              AND course_id = #{courseId}
            ORDER BY created_at DESC, id DESC
            LIMIT #{limit}
            """)
    List<RecentActivityRow> findRecentActivities(
            @Param("userId") Long userId,
            @Param("courseId") Long courseId,
            @Param("limit") Integer limit
    );

    @Update("""
            UPDATE courses
            SET
                progress_score = #{progressScore},
                updated_at = NOW()
            WHERE id = #{courseId}
              AND user_id = #{userId}
            """)
    int updateCourseProgressScore(
            @Param("userId") Long userId,
            @Param("courseId") Long courseId,
            @Param("progressScore") Double progressScore
    );
}