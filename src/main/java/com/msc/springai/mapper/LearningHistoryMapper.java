package com.msc.springai.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface LearningHistoryMapper {

    @Insert("""
            INSERT INTO learning_history (
                user_id,
                course_id,
                event_type,
                target_type,
                target_id,
                topic
            )
            VALUES (
                #{userId},
                #{courseId},
                #{eventType},
                #{targetType},
                #{targetId},
                #{topic}
            )
            """)
    int insertLearningHistory(
            @Param("userId") Long userId,
            @Param("courseId") Long courseId,
            @Param("eventType") String eventType,
            @Param("targetType") String targetType,
            @Param("targetId") Long targetId,
            @Param("topic") String topic
    );
}