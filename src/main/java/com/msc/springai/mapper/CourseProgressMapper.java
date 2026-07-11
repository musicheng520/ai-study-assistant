package com.msc.springai.mapper;

import com.msc.springai.dto.progress.CourseProgressDtos.CourseProgressHeaderRow;
import com.msc.springai.dto.progress.CourseProgressDtos.CourseProgressStatsRow;
import com.msc.springai.dto.progress.CourseProgressDtos.WeakTopicProgressRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CourseProgressMapper {

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
    CourseProgressHeaderRow findCourseHeader(
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
                    FROM flashcards f
                    WHERE f.user_id = #{userId}
                      AND f.course_id = #{courseId}
                ) AS flashcardCount,

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
                    FROM wrong_answers wa
                    WHERE wa.user_id = #{userId}
                      AND wa.course_id = #{courseId}
                      AND wa.resolved = 1
                ) AS resolvedWrongAnswerCount,

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
                    FROM learning_history lh
                    WHERE lh.user_id = #{userId}
                      AND lh.course_id = #{courseId}
                      AND lh.created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY)
                ) AS recentActivityCount
            """)
    CourseProgressStatsRow findProgressStats(
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
    List<WeakTopicProgressRow> findWeakTopics(
            @Param("userId") Long userId,
            @Param("courseId") Long courseId,
            @Param("limit") Integer limit
    );
}