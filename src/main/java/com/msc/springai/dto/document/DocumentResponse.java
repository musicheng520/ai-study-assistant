package com.msc.springai.dto.document;

import com.msc.springai.entity.CourseDocument;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DocumentResponse {

    private Long id;
    private Long courseId;
    private String originalFileName;
    private String fileType;
    private String documentType;
    private Long fileSize;
    private String status;
    private String errorMessage;
    private Integer totalPages;
    private Integer chunkCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static DocumentResponse from(CourseDocument document) {
        DocumentResponse response = new DocumentResponse();
        response.setId(document.getId());
        response.setCourseId(document.getCourseId());
        response.setOriginalFileName(document.getOriginalFileName());
        response.setFileType(document.getFileType());
        response.setDocumentType(document.getDocumentType());
        response.setFileSize(document.getFileSize());
        response.setStatus(document.getStatus());
        response.setErrorMessage(document.getErrorMessage());
        response.setTotalPages(document.getTotalPages());
        response.setChunkCount(document.getChunkCount());
        response.setCreatedAt(document.getCreatedAt());
        response.setUpdatedAt(document.getUpdatedAt());
        return response;
    }
}