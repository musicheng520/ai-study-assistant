package com.msc.springai.mapper;

import com.msc.springai.entity.QuizAttemptAnswer;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QuizAttemptAnswerMapper {

    @Insert("""
            INSERT INTO quiz_attempt_answers (
                attempt_id,
                question_id,
                user_answer,
                is_correct,
                created_at
            )
            VALUES (
                #{attemptId},
                #{questionId},
                #{userAnswer},
                #{isCorrect},
                NOW()
            )
            """)
    void insert(QuizAttemptAnswer answer);
}