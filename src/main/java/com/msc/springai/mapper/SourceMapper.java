package com.msc.springai.mapper;

import com.msc.springai.dto.rag.SourceChunkResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SourceMapper {

    @Select("""
        SELECT
            dc.id AS chunkId,
            dc.user_id AS userId,
            dc.course_id AS courseId,
            dc.document_id AS documentId,
            dc.chunk_index AS chunkIndex,
            dc.content AS content,
            dc.content_hash AS contentHash,
            dc.page_number AS pageNumber,
            dc.section_title AS sectionTitle,
            dc.token_count AS tokenCount,
            dc.vector_key AS vectorKey,
            dc.vector_status AS vectorStatus,
            dc.embedding_model AS embeddingModel,
            dc.embedding_dimension AS embeddingDimension,
            dc.created_at AS createdAt,

            d.original_file_name AS fileName,
            d.file_type AS fileType,
            d.document_type AS documentType
        FROM document_chunks dc
        JOIN documents d ON dc.document_id = d.id
        WHERE dc.id = #{chunkId}
          AND dc.user_id = #{userId}
          AND dc.course_id = #{courseId}
    """)
    SourceChunkResponse findChunkSource(
            @Param("userId") Long userId,
            @Param("courseId") Long courseId,
            @Param("chunkId") Long chunkId
    );
}