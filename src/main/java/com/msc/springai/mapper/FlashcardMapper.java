package com.msc.springai.mapper;

import com.msc.springai.entity.Flashcard;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface FlashcardMapper {

    @Insert("""
            INSERT INTO flashcards (
                user_id,
                course_id,
                document_id,
                front,
                back,
                topic,
                difficulty,
                source_type,
                source_chunk_id,
                created_at
            )
            VALUES (
                #{userId},
                #{courseId},
                #{documentId},
                #{front},
                #{back},
                #{topic},
                #{difficulty},
                #{sourceType},
                #{sourceChunkId},
                NOW()
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Flashcard flashcard);

    @Select("""
            SELECT *
            FROM flashcards
            WHERE user_id = #{userId}
              AND course_id = #{courseId}
            ORDER BY created_at DESC, id DESC
            """)
    List<Flashcard> findByUserIdAndCourseId(
            @Param("userId") Long userId,
            @Param("courseId") Long courseId
    );

    @Select("""
            SELECT *
            FROM flashcards
            WHERE id = #{flashcardId}
              AND user_id = #{userId}
            """)
    Flashcard findByIdAndUserId(
            @Param("flashcardId") Long flashcardId,
            @Param("userId") Long userId
    );

    @Delete("""
            DELETE FROM flashcards
            WHERE id = #{flashcardId}
              AND user_id = #{userId}
            """)
    int deleteByIdAndUserId(
            @Param("flashcardId") Long flashcardId,
            @Param("userId") Long userId
    );
}