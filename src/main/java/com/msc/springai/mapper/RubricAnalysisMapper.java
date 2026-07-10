package com.msc.springai.mapper;

import com.msc.springai.entity.RubricAnalysis;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface RubricAnalysisMapper {

    @Insert("""
        INSERT INTO rubric_analyses (
            user_id,
            course_id,
            document_id,
            criteria_json,
            excellent_band_json,
            common_mistakes,
            high_score_strategy,
            created_at
        )
        VALUES (
            #{userId},
            #{courseId},
            #{documentId},
            #{criteriaJson},
            #{excellentBandJson},
            #{commonMistakes},
            #{highScoreStrategy},
            #{createdAt}
        )
    """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(RubricAnalysis analysis);

    @Select("""
        SELECT
            id,
            user_id AS userId,
            course_id AS courseId,
            document_id AS documentId,
            criteria_json AS criteriaJson,
            excellent_band_json AS excellentBandJson,
            common_mistakes AS commonMistakes,
            high_score_strategy AS highScoreStrategy,
            created_at AS createdAt
        FROM rubric_analyses
        WHERE user_id = #{userId}
          AND course_id = #{courseId}
        ORDER BY created_at DESC, id DESC
    """)
    List<RubricAnalysis> findByCourseIdAndUserId(@Param("userId") Long userId,
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