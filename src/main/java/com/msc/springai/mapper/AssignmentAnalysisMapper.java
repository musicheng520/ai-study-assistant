package com.msc.springai.mapper;

import com.msc.springai.entity.AssignmentAnalysis;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AssignmentAnalysisMapper {

    @Insert("""
        INSERT INTO assignment_analyses (
            user_id,
            course_id,
            document_id,
            requirements_json,
            deliverables_json,
            deadline,
            checklist_json,
            high_score_tips,
            suggested_structure_json,
            risk_warnings_json,
            created_at
        )
        VALUES (
            #{userId},
            #{courseId},
            #{documentId},
            #{requirementsJson},
            #{deliverablesJson},
            #{deadline},
            #{checklistJson},
            #{highScoreTips},
            #{suggestedStructureJson},
            #{riskWarningsJson},
            #{createdAt}
        )
    """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AssignmentAnalysis analysis);

    @Select("""
        SELECT
            id,
            user_id AS userId,
            course_id AS courseId,
            document_id AS documentId,
            requirements_json AS requirementsJson,
            deliverables_json AS deliverablesJson,
            deadline,
            checklist_json AS checklistJson,
            high_score_tips AS highScoreTips,
            suggested_structure_json AS suggestedStructureJson,
            risk_warnings_json AS riskWarningsJson,
            created_at AS createdAt
        FROM assignment_analyses
        WHERE user_id = #{userId}
          AND course_id = #{courseId}
        ORDER BY created_at DESC, id DESC
    """)
    List<AssignmentAnalysis> findByCourseIdAndUserId(@Param("userId") Long userId,
                                                     @Param("courseId") Long courseId);

    @Select("""
        SELECT COUNT(*)
        FROM courses
        WHERE id = #{courseId}
          AND user_id = #{userId}
    """)
    int countCourseOwnership(@Param("userId") Long userId,
                             @Param("courseId") Long courseId);

    @Insert("""
        INSERT INTO learning_history (
            user_id,
            course_id,
            event_type,
            target_type,
            target_id,
            topic,
            created_at
        )
        VALUES (
            #{userId},
            #{courseId},
            #{eventType},
            #{targetType},
            #{targetId},
            #{topic},
            #{createdAt}
        )
    """)
    int insertLearningHistory(@Param("userId") Long userId,
                              @Param("courseId") Long courseId,
                              @Param("eventType") String eventType,
                              @Param("targetType") String targetType,
                              @Param("targetId") Long targetId,
                              @Param("topic") String topic,
                              @Param("createdAt") LocalDateTime createdAt);
}