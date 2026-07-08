package com.msc.springai.mapper;

import com.msc.springai.entity.QuizQuestion;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface QuizQuestionMapper {

    @Insert("""
            INSERT INTO quiz_questions (
                quiz_id,
                question_type,
                question_text,
                options_json,
                correct_answer,
                explanation,
                difficulty,
                topic,
                source_chunk_id,
                created_at
            )
            VALUES (
                #{quizId},
                #{questionType},
                #{questionText},
                CAST(#{optionsJson} AS JSON),
                #{correctAnswer},
                #{explanation},
                #{difficulty},
                #{topic},
                #{sourceChunkId},
                NOW()
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(QuizQuestion question);

    @Select("""
            SELECT *
            FROM quiz_questions
            WHERE quiz_id = #{quizId}
            ORDER BY id ASC
            """)
    List<QuizQuestion> findByQuizId(@Param("quizId") Long quizId);

    @Delete("""
            DELETE FROM quiz_questions
            WHERE quiz_id = #{quizId}
            """)
    int deleteByQuizId(@Param("quizId") Long quizId);
}