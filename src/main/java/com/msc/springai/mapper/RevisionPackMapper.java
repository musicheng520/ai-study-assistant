package com.msc.springai.mapper;

import com.msc.springai.entity.RevisionPack;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface RevisionPackMapper {

    @Select("""
        SELECT COUNT(*)
        FROM courses
        WHERE id = #{courseId}
          AND user_id = #{userId}
    """)
    int countCourseOwnership(@Param("userId") Long userId,
                             @Param("courseId") Long courseId);

    @Insert("""
        INSERT INTO revision_packs (
            user_id,
            course_id,
            title,
            summary,
            weak_topics_json,
            review_order_json,
            recommended_actions_json,
            related_documents_json,
            study_tasks_json,
            suggested_flashcards_json,
            generated_quiz_id,
            created_at
        )
        VALUES (
            #{userId},
            #{courseId},
            #{title},
            #{summary},
            #{weakTopicsJson},
            #{reviewOrderJson},
            #{recommendedActionsJson},
            #{relatedDocumentsJson},
            #{studyTasksJson},
            #{suggestedFlashcardsJson},
            #{generatedQuizId},
            #{createdAt}
        )
    """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(RevisionPack revisionPack);

    @Select("""
        SELECT
            id,
            user_id AS userId,
            course_id AS courseId,
            title,
            summary,
            weak_topics_json AS weakTopicsJson,
            review_order_json AS reviewOrderJson,
            recommended_actions_json AS recommendedActionsJson,
            related_documents_json AS relatedDocumentsJson,
            study_tasks_json AS studyTasksJson,
            suggested_flashcards_json AS suggestedFlashcardsJson,
            generated_quiz_id AS generatedQuizId,
            created_at AS createdAt
        FROM revision_packs
        WHERE user_id = #{userId}
          AND course_id = #{courseId}
        ORDER BY created_at DESC, id DESC
    """)
    List<RevisionPack> findByCourseIdAndUserId(@Param("userId") Long userId,
                                               @Param("courseId") Long courseId);

    @Select("""
        SELECT
            id,
            user_id AS userId,
            course_id AS courseId,
            title,
            summary,
            weak_topics_json AS weakTopicsJson,
            review_order_json AS reviewOrderJson,
            recommended_actions_json AS recommendedActionsJson,
            related_documents_json AS relatedDocumentsJson,
            study_tasks_json AS studyTasksJson,
            suggested_flashcards_json AS suggestedFlashcardsJson,
            generated_quiz_id AS generatedQuizId,
            created_at AS createdAt
        FROM revision_packs
        WHERE id = #{packId}
          AND user_id = #{userId}
    """)
    RevisionPack findByIdAndUserId(@Param("packId") Long packId,
                                   @Param("userId") Long userId);

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