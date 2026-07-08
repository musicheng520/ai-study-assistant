package com.msc.springai.mapper;

import com.msc.springai.entity.Summary;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface SummaryMapper {

    @Insert("""
            INSERT INTO summaries (
                user_id,
                course_id,
                document_id,
                title,
                summary,
                key_concepts_json,
                definitions_json,
                revision_notes,
                source_scope
            )
            VALUES (
                #{userId},
                #{courseId},
                #{documentId},
                #{title},
                #{summary},
                CAST(#{keyConceptsJson} AS JSON),
                CAST(#{definitionsJson} AS JSON),
                #{revisionNotes},
                #{sourceScope}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Summary summary);

    @Select("""
            SELECT
                id,
                user_id AS userId,
                course_id AS courseId,
                document_id AS documentId,
                title,
                summary,
                key_concepts_json AS keyConceptsJson,
                definitions_json AS definitionsJson,
                revision_notes AS revisionNotes,
                source_scope AS sourceScope,
                created_at AS createdAt
            FROM summaries
            WHERE user_id = #{userId}
              AND course_id = #{courseId}
            ORDER BY created_at DESC
            """)
    List<Summary> findByUserIdAndCourseId(
            @Param("userId") Long userId,
            @Param("courseId") Long courseId
    );

    @Select("""
            SELECT
                id,
                user_id AS userId,
                course_id AS courseId,
                document_id AS documentId,
                title,
                summary,
                key_concepts_json AS keyConceptsJson,
                definitions_json AS definitionsJson,
                revision_notes AS revisionNotes,
                source_scope AS sourceScope,
                created_at AS createdAt
            FROM summaries
            WHERE id = #{summaryId}
              AND user_id = #{userId}
            """)
    Summary findByIdAndUserId(
            @Param("summaryId") Long summaryId,
            @Param("userId") Long userId
    );

    @Delete("""
            DELETE FROM summaries
            WHERE id = #{summaryId}
              AND user_id = #{userId}
            """)
    int deleteByIdAndUserId(
            @Param("summaryId") Long summaryId,
            @Param("userId") Long userId
    );
}