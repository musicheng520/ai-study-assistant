package com.msc.springai.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

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
}