package com.msc.springai.mapper;

import com.msc.springai.entity.StudyTask;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface StudyTaskMapper {

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
    @Options(
            useGeneratedKeys = true,
            keyProperty = "id"
    )
    void insert(StudyTask task);

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
                    ELSE 1
                END,
                due_date ASC,
                created_at DESC
            """)
    List<StudyTask> findByUserIdAndCourseId(
            @Param("userId") Long userId,
            @Param("courseId") Long courseId
    );

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
    StudyTask findByIdAndUserId(
            @Param("taskId") Long taskId,
            @Param("userId") Long userId
    );

    @Update("""
            UPDATE study_tasks
            SET
                title = #{title},
                description = #{description},
                due_date = #{dueDate},
                source_type = #{sourceType},
                updated_at = #{updatedAt}
            WHERE id = #{id}
              AND user_id = #{userId}
            """)
    int updateByIdAndUserId(StudyTask task);

    @Update("""
            UPDATE study_tasks
            SET
                status = #{status},
                updated_at = #{updatedAt}
            WHERE id = #{taskId}
              AND user_id = #{userId}
            """)
    int updateStatusByIdAndUserId(
            @Param("taskId") Long taskId,
            @Param("userId") Long userId,
            @Param("status") String status,
            @Param("updatedAt") java.time.LocalDateTime updatedAt
    );

    @Delete("""
            DELETE FROM study_tasks
            WHERE id = #{taskId}
              AND user_id = #{userId}
            """)
    int deleteByIdAndUserId(
            @Param("taskId") Long taskId,
            @Param("userId") Long userId
    );

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

    @Select("""
            SELECT COUNT(*)
            FROM documents
            WHERE id = #{documentId}
              AND user_id = #{userId}
              AND course_id = #{courseId}
            """)
    int countDocumentOwnership(
            @Param("userId") Long userId,
            @Param("courseId") Long courseId,
            @Param("documentId") Long documentId
    );
}