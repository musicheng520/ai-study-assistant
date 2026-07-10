package com.msc.springai.mapper;

import com.msc.springai.dto.workflow.tool.StudyToolDtos.CourseDocumentToolResult;
import com.msc.springai.dto.workflow.tool.StudyToolDtos.LearningHistoryToolResult;
import com.msc.springai.dto.workflow.tool.StudyToolDtos.NoteToolResult;
import com.msc.springai.dto.workflow.tool.StudyToolDtos.WrongAnswerItem;
import com.msc.springai.dto.workflow.tool.StudyToolDtos.WrongAnswerTopicSummary;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface StudyAgentToolMapper {

    @Select("""
        SELECT COUNT(*)
        FROM courses
        WHERE id = #{courseId}
          AND user_id = #{userId}
    """)
    int countCourseOwnership(@Param("userId") Long userId,
                             @Param("courseId") Long courseId);

    @Select("""
        SELECT
            id AS documentId,
            original_file_name AS originalFileName,
            document_type AS documentType,
            status,
            chunk_count AS chunkCount,
            processed_at AS processedAt
        FROM documents
        WHERE user_id = #{userId}
          AND course_id = #{courseId}
          AND status = 'READY'
          AND (#{documentType} IS NULL OR document_type = #{documentType})
        ORDER BY processed_at DESC, id DESC
    """)
    List<CourseDocumentToolResult> findReadyDocuments(
            @Param("userId") Long userId,
            @Param("courseId") Long courseId,
            @Param("documentType") String documentType
    );

    @Select("""
        SELECT
            event_type AS eventType,
            target_type AS targetType,
            target_id AS targetId,
            topic,
            created_at AS createdAt
        FROM learning_history
        WHERE user_id = #{userId}
          AND course_id = #{courseId}
        ORDER BY created_at DESC
        LIMIT #{limit}
    """)
    List<LearningHistoryToolResult> findRecentLearningHistory(
            @Param("userId") Long userId,
            @Param("courseId") Long courseId,
            @Param("limit") Integer limit
    );

    @Select("""
        SELECT
            COALESCE(topic, 'Unknown') AS topic,
            COUNT(*) AS wrongCount,
            SUM(CASE WHEN resolved = FALSE THEN 1 ELSE 0 END) AS unresolvedCount
        FROM wrong_answers
        WHERE user_id = #{userId}
          AND course_id = #{courseId}
          AND (#{resolved} IS NULL OR resolved = #{resolved})
        GROUP BY COALESCE(topic, 'Unknown')
        ORDER BY unresolvedCount DESC, wrongCount DESC
        LIMIT #{limit}
    """)
    List<WrongAnswerTopicSummary> findWeakTopicSummaries(
            @Param("userId") Long userId,
            @Param("courseId") Long courseId,
            @Param("resolved") Boolean resolved,
            @Param("limit") Integer limit
    );

    @Select("""
        SELECT
            id AS wrongAnswerId,
            quiz_id AS quizId,
            question_id AS questionId,
            topic,
            user_answer AS userAnswer,
            correct_answer AS correctAnswer,
            explanation,
            resolved,
            created_at AS createdAt
        FROM wrong_answers
        WHERE user_id = #{userId}
          AND course_id = #{courseId}
          AND (#{resolved} IS NULL OR resolved = #{resolved})
        ORDER BY resolved ASC, created_at DESC
        LIMIT #{limit}
    """)
    List<WrongAnswerItem> findWrongAnswerItems(
            @Param("userId") Long userId,
            @Param("courseId") Long courseId,
            @Param("resolved") Boolean resolved,
            @Param("limit") Integer limit
    );

    @Select("""
        SELECT
            id AS noteId,
            title,
            content,
            topic,
            document_id AS documentId,
            created_at AS createdAt
        FROM notes
        WHERE user_id = #{userId}
          AND course_id = #{courseId}
          AND (
                #{keyword} IS NULL
                OR title LIKE CONCAT('%', #{keyword}, '%')
                OR content LIKE CONCAT('%', #{keyword}, '%')
              )
          AND (
                #{topic} IS NULL
                OR topic = #{topic}
              )
        ORDER BY updated_at DESC, created_at DESC
        LIMIT #{limit}
    """)
    List<NoteToolResult> searchNotes(
            @Param("userId") Long userId,
            @Param("courseId") Long courseId,
            @Param("keyword") String keyword,
            @Param("topic") String topic,
            @Param("limit") Integer limit
    );

    @Select("""
        SELECT COALESCE(progress_score, 0)
        FROM courses
        WHERE id = #{courseId}
          AND user_id = #{userId}
    """)
    BigDecimal findCourseProgressScore(
            @Param("userId") Long userId,
            @Param("courseId") Long courseId
    );

    @Select("""
        SELECT COALESCE(AVG(score), 0)
        FROM quiz_attempts
        WHERE user_id = #{userId}
          AND course_id = #{courseId}
    """)
    BigDecimal findAverageQuizScore(
            @Param("userId") Long userId,
            @Param("courseId") Long courseId
    );

    @Select("""
        SELECT COUNT(*)
        FROM wrong_answers
        WHERE user_id = #{userId}
          AND course_id = #{courseId}
    """)
    int countWrongAnswers(
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
    int countUnresolvedWrongAnswers(
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
    int countReadyDocumentsInCourse(@Param("userId") Long userId,
                                    @Param("courseId") Long courseId);

    @Select("""
    SELECT COUNT(*)
    FROM documents
    WHERE id = #{documentId}
      AND user_id = #{userId}
      AND course_id = #{courseId}
      AND status = 'READY'
""")
    int countReadyDocumentAccess(@Param("userId") Long userId,
                                 @Param("courseId") Long courseId,
                                 @Param("documentId") Long documentId);
}