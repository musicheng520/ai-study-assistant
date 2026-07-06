package com.msc.springai.mapper;

import com.msc.springai.entity.DocumentProcessingJob;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;

@Mapper
public interface DocumentProcessingJobMapper {

    @Insert("""
            INSERT INTO document_processing_jobs (
                user_id, course_id, document_id, status, step, retry_count
            )
            VALUES (
                #{userId}, #{courseId}, #{documentId}, #{status}, #{step}, #{retryCount}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(DocumentProcessingJob job);
}