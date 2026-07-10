package com.msc.springai.mapper;

import com.msc.springai.entity.StudyTask;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface StudyTaskMapper {

    @Select("""
        SELECT COUNT(*)
        FROM courses
        WHERE id = #{courseId}
          AND user_id = #{userId}
    """)
    int countCourseOwnership(@Param("userId") Long userId,
                             @Param("courseId") Long courseId);

    @Select("""
        SELECT COUNT(*)
        FROM documents
        WHERE id = #{documentId}
          AND user_id = #{userId}
          AND course_id = #{courseId}
    """)
    int countDocumentOwnership(@Param("userId") Long userId,
                               @Param("courseId") Long courseId,
                               @Param("documentId") Long documentId);

    @Insert("""
        INSERT INTO study_tasks (
            user_id,
            course_id,
            document_id,
            title,
            description,
            status,
            due_date,
            source_type,
            created_at,
            updated_at
        )
        VALUES (
            #{userId},
            #{courseId},
            #{documentId},
            #{title},
            #{description},
            #{status},
            #{dueDate},
            #{sourceType},
            #{createdAt},
            #{updatedAt}
        )
    """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(StudyTask task);

    @Select("""
        SELECT
            id,
            user_id AS userId,
            course_id AS courseId,
            document_id AS documentId,
            title,
            description,
            status,
            due_date AS dueDate,
            source_type AS sourceType,
            created_at AS createdAt,
            updated_at AS updatedAt
        FROM study_tasks
        WHERE user_id = #{userId}
          AND course_id = #{courseId}
        ORDER BY
            CASE status
                WHEN 'TODO' THEN 0
                WHEN 'DONE' THEN 1
                ELSE 2
            END,
            due_date IS NULL,
            due_date ASC,
            created_at DESC
    """)
    List<StudyTask> findByCourseIdAndUserId(@Param("userId") Long userId,
                                            @Param("courseId") Long courseId);

    @Select("""
        SELECT
            id,
            user_id AS userId,
            course_id AS courseId,
            document_id AS documentId,
            title,
            description,
            status,
            due_date AS dueDate,
            source_type AS sourceType,
            created_at AS createdAt,
            updated_at AS updatedAt
        FROM study_tasks
        WHERE id = #{taskId}
          AND user_id = #{userId}
    """)
    StudyTask findByIdAndUserId(@Param("taskId") Long taskId,
                                @Param("userId") Long userId);

    @Update("""
        UPDATE study_tasks
        SET
            title = #{title},
            description = #{description},
            due_date = #{dueDate},
            updated_at = #{updatedAt}
        WHERE id = #{id}
          AND user_id = #{userId}
    """)
    int update(StudyTask task);

    @Update("""
        UPDATE study_tasks
        SET
            status = 'DONE',
            updated_at = #{updatedAt}
        WHERE id = #{taskId}
          AND user_id = #{userId}
    """)
    int markDone(@Param("taskId") Long taskId,
                 @Param("userId") Long userId,
                 @Param("updatedAt") LocalDateTime updatedAt);

    @Delete("""
        DELETE FROM study_tasks
        WHERE id = #{taskId}
          AND user_id = #{userId}
    """)
    int deleteByIdAndUserId(@Param("taskId") Long taskId,
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

    @Select("""
    SELECT COUNT(*)
    FROM study_tasks
    WHERE user_id = #{userId}
      AND course_id = #{courseId}
      AND title = #{title}
""")
    int countByTitle(@Param("userId") Long userId,
                     @Param("courseId") Long courseId,
                     @Param("title") String title);
}