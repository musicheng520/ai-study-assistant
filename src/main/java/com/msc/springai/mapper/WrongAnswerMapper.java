package com.msc.springai.mapper;

import com.msc.springai.entity.WrongAnswer;
import org.apache.ibatis.annotations.*;

import java.util.List;

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

    @Select("""
            <script>
            SELECT *
            FROM wrong_answers
            WHERE user_id = #{userId}
              AND course_id = #{courseId}
            <if test="resolved != null">
              AND resolved = #{resolved}
            </if>
            <if test="topic != null and topic != ''">
              AND topic = #{topic}
            </if>
            ORDER BY created_at DESC, id DESC
            </script>
            """)
    List<WrongAnswer> findByCourse(
            @Param("userId") Long userId,
            @Param("courseId") Long courseId,
            @Param("resolved") Boolean resolved,
            @Param("topic") String topic
    );

    @Select("""
            SELECT *
            FROM wrong_answers
            WHERE id = #{wrongAnswerId}
              AND user_id = #{userId}
            """)
    WrongAnswer findByIdAndUserId(
            @Param("wrongAnswerId") Long wrongAnswerId,
            @Param("userId") Long userId
    );

    @Update("""
            UPDATE wrong_answers
            SET resolved = TRUE
            WHERE id = #{wrongAnswerId}
              AND user_id = #{userId}
            """)
    int markResolved(
            @Param("wrongAnswerId") Long wrongAnswerId,
            @Param("userId") Long userId
    );

    @Delete("""
            DELETE FROM wrong_answers
            WHERE id = #{wrongAnswerId}
              AND user_id = #{userId}
            """)
    int deleteByIdAndUserId(
            @Param("wrongAnswerId") Long wrongAnswerId,
            @Param("userId") Long userId
    );
}