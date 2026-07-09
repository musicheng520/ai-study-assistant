package com.msc.springai.mapper;

import com.msc.springai.entity.WrongAnswer;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;

@Mapper
public interface WrongAnswerMapper {

    @Insert("""
            INSERT INTO wrong_answers (
                user_id,
                course_id,
                quiz_id,
                question_id,
                topic,
                user_answer,
                correct_answer,
                explanation,
                resolved,
                created_at
            )
            VALUES (
                #{userId},
                #{courseId},
                #{quizId},
                #{questionId},
                #{topic},
                #{userAnswer},
                #{correctAnswer},
                #{explanation},
                FALSE,
                NOW()
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(WrongAnswer wrongAnswer);
}