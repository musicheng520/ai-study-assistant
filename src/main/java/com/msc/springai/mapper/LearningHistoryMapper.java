package com.msc.springai.mapper;

import com.msc.springai.dto.learning.history.LearningHistoryTypeCountResponse;
import com.msc.springai.entity.LearningHistory;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface LearningHistoryMapper {

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
    void insertLearningHistory(
            @Param("userId") Long userId,
            @Param("courseId") Long courseId,
            @Param("eventType") String eventType,
            @Param("targetType") String targetType,
            @Param("targetId") Long targetId,
            @Param("topic") String topic,
            @Param("createdAt") LocalDateTime createdAt
    );

    default void insertLearningHistory(
            Long userId,
            Long courseId,
            String eventType,
            String targetType,
            Long targetId,
            String topic
    ) {
        insertLearningHistory(
                userId,
                courseId,
                eventType,
                targetType,
                targetId,
                topic,
                LocalDateTime.now()
        );
    }

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
            SELECT
                id,
                user_id AS userId,
                course_id AS courseId,
                event_type AS eventType,
                target_type AS targetType,
                target_id AS targetId,
                topic,
                created_at AS createdAt
            FROM learning_history
            WHERE user_id = #{userId}
            ORDER BY created_at DESC, id DESC
            LIMIT #{limit}
            OFFSET #{offset}
            """)
    List<LearningHistory> findRecentByUserId(
            @Param("userId") Long userId,
            @Param("limit") Integer limit,
            @Param("offset") Integer offset
    );

    @Select("""
            SELECT
                id,
                user_id AS userId,
                course_id AS courseId,
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
            OFFSET #{offset}
            """)
    List<LearningHistory> findByUserIdAndCourseId(
            @Param("userId") Long userId,
            @Param("courseId") Long courseId,
            @Param("limit") Integer limit,
            @Param("offset") Integer offset
    );

    @Select("""
            SELECT COUNT(*)
            FROM learning_history
            WHERE user_id = #{userId}
              AND course_id = #{courseId}
            """)
    Long countByUserIdAndCourseId(
            @Param("userId") Long userId,
            @Param("courseId") Long courseId
    );

    @Select("""
            SELECT MAX(created_at)
            FROM learning_history
            WHERE user_id = #{userId}
              AND course_id = #{courseId}
            """)
    LocalDateTime findLatestActivityAt(
            @Param("userId") Long userId,
            @Param("courseId") Long courseId
    );

    @Select("""
            SELECT
                event_type AS eventType,
                COUNT(*) AS count
            FROM learning_history
            WHERE user_id = #{userId}
              AND course_id = #{courseId}
            GROUP BY event_type
            ORDER BY count DESC, event_type ASC
            """)
    List<LearningHistoryTypeCountResponse> countByEventType(
            @Param("userId") Long userId,
            @Param("courseId") Long courseId
    );
}