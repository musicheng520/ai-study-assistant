package com.msc.springai.service;

import com.msc.springai.dto.rag.SourceChunkResponse;
import com.msc.springai.entity.Course;
import com.msc.springai.mapper.CourseMapper;
import com.msc.springai.mapper.SourceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SourceService {

    private final CourseMapper courseMapper;
    private final SourceMapper sourceMapper;

    public SourceChunkResponse getChunkSource(
            Long userId,
            Long courseId,
            Long chunkId
    ) {
        System.out.println("[SourceService] Start getting chunk source.");
        System.out.println("[SourceService] userId = " + userId);
        System.out.println("[SourceService] courseId = " + courseId);
        System.out.println("[SourceService] chunkId = " + chunkId);

        if (userId == null) {
            throw new RuntimeException("User id is required");
        }

        if (courseId == null) {
            throw new RuntimeException("Course id is required");
        }

        if (chunkId == null) {
            throw new RuntimeException("Chunk id is required");
        }

        Course course = courseMapper.findByIdAndUserId(courseId, userId);

        if (course == null) {
            throw new RuntimeException("Course not found or access denied");
        }

        System.out.println("[SourceService] Course verified.");

        SourceChunkResponse source = sourceMapper.findChunkSource(
                userId,
                courseId,
                chunkId
        );

        if (source == null) {
            throw new RuntimeException("Source chunk not found or access denied");
        }

        System.out.println("[SourceService] Source chunk found.");
        System.out.println("[SourceService] documentId = " + source.getDocumentId());
        System.out.println("[SourceService] fileName = " + source.getFileName());
        System.out.println("[SourceService] pageNumber = " + source.getPageNumber());
        System.out.println("[SourceService] content length = "
                + (source.getContent() == null ? 0 : source.getContent().length()));

        return source;
    }
}