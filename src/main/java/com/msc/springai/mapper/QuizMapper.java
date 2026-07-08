package com.msc.springai.mapper;

import com.msc.springai.entity.Quiz;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface QuizMapper {

    @Insert("""
            INSERT INTO quizzes (
                user_id,
                course_id,
                document_id,
                title,
                difficulty,
                source_scope,
                question_count,
                created_at
            )
            VALUES (
                #{userId},
                #{courseId},
                #{documentId},
                #{title},
                #{difficulty},
                #{sourceScope},
                #{questionCount},
                NOW()
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Quiz quiz);

    @Select("""
            SELECT *
            FROM quizzes
            WHERE user_id = #{userId}
              AND course_id = #{courseId}
            ORDER BY created_at DESC
            """)
    List<Quiz> findByUserIdAndCourseId(
            @Param("userId") Long userId,
            @Param("courseId") Long courseId
    );

    @Select("""
            SELECT *
            FROM quizzes
            WHERE id = #{quizId}
              AND user_id = #{userId}
            """)
    Quiz findByIdAndUserId(
            @Param("quizId") Long quizId,
            @Param("userId") Long userId
    );

    @Delete("""
            DELETE FROM quizzes
            WHERE id = #{quizId}
              AND user_id = #{userId}
            """)
    int deleteByIdAndUserId(
            @Param("quizId") Long quizId,
            @Param("userId") Long userId
    );
}