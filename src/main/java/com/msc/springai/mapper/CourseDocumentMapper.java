package com.msc.springai.mapper;

import com.msc.springai.entity.CourseDocument;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface CourseDocumentMapper {

    @Insert("""
            INSERT INTO documents (
                user_id, course_id, original_file_name, stored_file_path,
                file_type, document_type, file_size, status, version, chunk_count
            )
            VALUES (
                #{userId}, #{courseId}, #{originalFileName}, #{storedFilePath},
                #{fileType}, #{documentType}, #{fileSize}, #{status}, #{version}, #{chunkCount}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(CourseDocument document);

    @Select("""
            SELECT id, user_id, course_id, original_file_name, stored_file_path,
                   file_type, document_type, file_size, status, error_message,
                   version, total_pages, chunk_count, processed_at, created_at, updated_at
            FROM documents
            WHERE id = #{id}
            """)
    CourseDocument findById(Long id);

    @Select("""
            SELECT id, user_id, course_id, original_file_name, stored_file_path,
                   file_type, document_type, file_size, status, error_message,
                   version, total_pages, chunk_count, processed_at, created_at, updated_at
            FROM documents
            WHERE course_id = #{courseId}
              AND user_id = #{userId}
            ORDER BY created_at DESC
            """)
    List<CourseDocument> findByCourseIdAndUserId(@Param("courseId") Long courseId,
                                                 @Param("userId") Long userId);
}