package com.msc.springai.dev;

import com.msc.springai.dto.learning.retrieval.RetrievedChunk;
import com.msc.springai.service.RetrievalService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dev/retrieval")
@RequiredArgsConstructor
public class RetrievalTestController {

    private final RetrievalService retrievalService;

    @GetMapping("/course")
    public Map<String, Object> retrieveCourseChunks(
            @RequestParam Long userId,
            @RequestParam Long courseId,
            @RequestParam(defaultValue = "8") Integer topK,
            @RequestParam(required = false) String query
    ) {
        List<RetrievedChunk> chunks = retrievalService.retrieveCourseChunks(
                userId,
                courseId,
                topK,
                query
        );

        return Map.of(
                "scope", "COURSE",
                "userId", userId,
                "courseId", courseId,
                "topK", topK,
                "count", chunks.size(),
                "chunks", chunks
        );
    }

    @GetMapping("/document")
    public Map<String, Object> retrieveDocumentChunks(
            @RequestParam Long userId,
            @RequestParam Long courseId,
            @RequestParam Long documentId,
            @RequestParam(defaultValue = "8") Integer topK,
            @RequestParam(required = false) String query
    ) {
        List<RetrievedChunk> chunks = retrievalService.retrieveDocumentChunks(
                userId,
                courseId,
                documentId,
                topK,
                query
        );

        return Map.of(
                "scope", "DOCUMENT",
                "userId", userId,
                "courseId", courseId,
                "documentId", documentId,
                "topK", topK,
                "count", chunks.size(),
                "chunks", chunks
        );
    }
}