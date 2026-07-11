package com.msc.springai.mapper;

import com.msc.springai.entity.AiFeedback;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AiFeedbackMapper {

    /*
     * 保存一条用户反馈。
     *
     * userId 必须由 JWT 得到，
     * 不能直接相信前端传入的数据。
     */
    @Insert("""
            INSERT INTO ai_feedback (
                user_id,
                course_id,
                target_type,
                target_id,
                rating,
                comment,
                created_at
            )
            VALUES (
                #{userId},
                #{courseId},
                #{targetType},
                #{targetId},
                #{rating},
                #{comment},
                #{createdAt}
            )
            """)
    @Options(
            useGeneratedKeys = true,
            keyProperty = "id"
    )
    void insert(AiFeedback feedback);

    /*
     * 查询当前用户提交的全部反馈。
     *
     * 不允许 Controller 从请求参数接收其他 userId。
     * Service 必须把 JWT 中的 currentUserId 传进来。
     */
    @Select("""
            SELECT
                id,
                user_id AS userId,
                course_id AS courseId,
                target_type AS targetType,
                target_id AS targetId,
                rating,
                comment,
                created_at AS createdAt
            FROM ai_feedback
            WHERE user_id = #{userId}
            ORDER BY created_at DESC, id DESC
            """)
    List<AiFeedback> findByUserId(
            @Param("userId") Long userId
    );

    /*
     * 检查 course 是否属于当前用户。
     *
     * 在检查具体 AI target 之前，
     * 先确保 course 本身合法。
     */
    @Select("""
            SELECT COUNT(*)
            FROM courses
            WHERE id = #{courseId}
              AND user_id = #{userId}
            """)
    int countCourseOwnership(
            @Param("userId") Long userId,
            @Param("courseId") Long courseId
    );

    /*
     * ANSWER 对应 chat_messages.id。
     *
     * role 必须是 ASSISTANT，
     * 防止用户给自己的 USER 问题消息提交 AI 反馈。
     */
    @Select("""
            SELECT COUNT(*)
            FROM chat_messages
            WHERE id = #{targetId}
              AND user_id = #{userId}
              AND course_id = #{courseId}
              AND role = 'ASSISTANT'
            """)
    int countAnswerTarget(
            @Param("userId") Long userId,
            @Param("courseId") Long courseId,
            @Param("targetId") Long targetId
    );

    /*
     * SUMMARY 对应 summaries.id。
     */
    @Select("""
            SELECT COUNT(*)
            FROM summaries
            WHERE id = #{targetId}
              AND user_id = #{userId}
              AND course_id = #{courseId}
            """)
    int countSummaryTarget(
            @Param("userId") Long userId,
            @Param("courseId") Long courseId,
            @Param("targetId") Long targetId
    );

    /*
     * QUIZ 对应 quizzes.id。
     */
    @Select("""
            SELECT COUNT(*)
            FROM quizzes
            WHERE id = #{targetId}
              AND user_id = #{userId}
              AND course_id = #{courseId}
            """)
    int countQuizTarget(
            @Param("userId") Long userId,
            @Param("courseId") Long courseId,
            @Param("targetId") Long targetId
    );

    /*
     * FLASHCARD 对应单条 flashcards.id。
     */
    @Select("""
            SELECT COUNT(*)
            FROM flashcards
            WHERE id = #{targetId}
              AND user_id = #{userId}
              AND course_id = #{courseId}
            """)
    int countFlashcardTarget(
            @Param("userId") Long userId,
            @Param("courseId") Long courseId,
            @Param("targetId") Long targetId
    );

    /*
     * ASSIGNMENT_ANALYSIS 对应 assignment_analyses.id。
     */
    @Select("""
            SELECT COUNT(*)
            FROM assignment_analyses
            WHERE id = #{targetId}
              AND user_id = #{userId}
              AND course_id = #{courseId}
            """)
    int countAssignmentAnalysisTarget(
            @Param("userId") Long userId,
            @Param("courseId") Long courseId,
            @Param("targetId") Long targetId
    );

    /*
     * RUBRIC_ANALYSIS 对应 rubric_analyses.id。
     */
    @Select("""
            SELECT COUNT(*)
            FROM rubric_analyses
            WHERE id = #{targetId}
              AND user_id = #{userId}
              AND course_id = #{courseId}
            """)
    int countRubricAnalysisTarget(
            @Param("userId") Long userId,
            @Param("courseId") Long courseId,
            @Param("targetId") Long targetId
    );

    /*
     * REVISION_PACK 对应 revision_packs.id。
     */
    @Select("""
            SELECT COUNT(*)
            FROM revision_packs
            WHERE id = #{targetId}
              AND user_id = #{userId}
              AND course_id = #{courseId}
            """)
    int countRevisionPackTarget(
            @Param("userId") Long userId,
            @Param("courseId") Long courseId,
            @Param("targetId") Long targetId
    );
}