package com.msc.springai.mapper;

import com.msc.springai.entity.DocumentChunk;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface DocumentChunkMapper {

    @Insert("""
        INSERT INTO document_chunks (
            user_id,
            course_id,
            document_id,
            chunk_index,
            content,
            content_hash,
            page_number,
            section_title,
            token_count,
            vector_key,
            vector_status,
            embedding_model,
            embedding_dimension,
            created_at
        ) VALUES (
            #{userId},
            #{courseId},
            #{documentId},
            #{chunkIndex},
            #{content},
            #{contentHash},
            #{pageNumber},
            #{sectionTitle},
            #{tokenCount},
            #{vectorKey},
            #{vectorStatus},
            #{embeddingModel},
            #{embeddingDimension},
            NOW()
        )
    """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(DocumentChunk chunk);

    default int insertBatch(List<DocumentChunk> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return 0;
        }

        int count = 0;
        for (DocumentChunk chunk : chunks) {
            count += insert(chunk);
        }
        return count;
    }

    @Select("""
        SELECT *
        FROM document_chunks
        WHERE id = #{chunkId}
          AND user_id = #{userId}
    """)
    @Results(id = "DocumentChunkResultMap", value = {
            @Result(property = "id", column = "id"),
            @Result(property = "userId", column = "user_id"),
            @Result(property = "courseId", column = "course_id"),
            @Result(property = "documentId", column = "document_id"),
            @Result(property = "chunkIndex", column = "chunk_index"),
            @Result(property = "content", column = "content"),
            @Result(property = "contentHash", column = "content_hash"),
            @Result(property = "pageNumber", column = "page_number"),
            @Result(property = "sectionTitle", column = "section_title"),
            @Result(property = "tokenCount", column = "token_count"),
            @Result(property = "vectorKey", column = "vector_key"),
            @Result(property = "vectorStatus", column = "vector_status"),
            @Result(property = "embeddingModel", column = "embedding_model"),
            @Result(property = "embeddingDimension", column = "embedding_dimension"),
            @Result(property = "createdAt", column = "created_at")
    })
    DocumentChunk findByIdAndUserId(
            @Param("chunkId") Long chunkId,
            @Param("userId") Long userId
    );

    @Select("""
        SELECT *
        FROM document_chunks
        WHERE document_id = #{documentId}
          AND user_id = #{userId}
        ORDER BY chunk_index ASC
    """)
    @ResultMap("DocumentChunkResultMap")
    List<DocumentChunk> findByDocumentIdAndUserId(
            @Param("documentId") Long documentId,
            @Param("userId") Long userId
    );

    @Select("""
        SELECT *
        FROM document_chunks
        WHERE course_id = #{courseId}
          AND user_id = #{userId}
        ORDER BY document_id ASC, chunk_index ASC
    """)
    @ResultMap("DocumentChunkResultMap")
    List<DocumentChunk> findByCourseIdAndUserId(
            @Param("courseId") Long courseId,
            @Param("userId") Long userId
    );

    @Delete("""
        DELETE FROM document_chunks
        WHERE document_id = #{documentId}
          AND user_id = #{userId}
    """)
    int deleteByDocumentIdAndUserId(
            @Param("documentId") Long documentId,
            @Param("userId") Long userId
    );

    @Update("""
        UPDATE document_chunks
        SET vector_key = #{vectorKey},
            vector_status = #{vectorStatus},
            embedding_model = #{embeddingModel},
            embedding_dimension = #{embeddingDimension}
        WHERE id = #{chunkId}
          AND user_id = #{userId}
    """)
    int updateVectorStatus(
            @Param("chunkId") Long chunkId,
            @Param("userId") Long userId,
            @Param("vectorKey") String vectorKey,
            @Param("vectorStatus") String vectorStatus,
            @Param("embeddingModel") String embeddingModel,
            @Param("embeddingDimension") Integer embeddingDimension
    );

    @Update("""
        UPDATE document_chunks
        SET vector_status = #{vectorStatus}
        WHERE document_id = #{documentId}
          AND user_id = #{userId}
    """)
    int updateVectorStatusByDocumentId(
            @Param("documentId") Long documentId,
            @Param("userId") Long userId,
            @Param("vectorStatus") String vectorStatus
    );

    @Select("""
        SELECT COUNT(*)
        FROM document_chunks
        WHERE document_id = #{documentId}
          AND user_id = #{userId}
    """)
    int countByDocumentIdAndUserId(
            @Param("documentId") Long documentId,
            @Param("userId") Long userId
    );
}