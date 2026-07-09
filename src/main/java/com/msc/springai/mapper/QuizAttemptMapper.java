package com.msc.springai.mapper;

import com.msc.springai.entity.QuizAttempt;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface QuizAttemptMapper {

    @Insert("""
            INSERT INTO quiz_attempts (
                user_id,
                course_id,
                quiz_id,
                score,
                total_questions,
                correct_count,
                started_at,
                submitted_at
            )
            VALUES (
                #{userId},
                #{courseId},
                #{quizId},
                #{score},
                #{totalQuestions},
                #{correctCount},
                #{startedAt},
                #{submittedAt}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(QuizAttempt attempt);

    @Select("""
            SELECT *
            FROM quiz_attempts
            WHERE user_id = #{userId}
              AND quiz_id = #{quizId}
            ORDER BY submitted_at DESC, id DESC
            """)
    List<QuizAttempt> findByUserIdAndQuizId(
            @Param("userId") Long userId,
            @Param("quizId") Long quizId
    );

    @Update("""
        UPDATE quiz_attempts
        SET correct_count = #{correctCount},
            score = #{score}
        WHERE id = #{attemptId}
        """)
    void updateScore(
            @Param("attemptId") Long attemptId,
            @Param("correctCount") Integer correctCount,
            @Param("score") Double score
    );
}