package com.msc.springai.dto.rag;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SourceChunkResponse {

    private Long chunkId;

    private Long userId;

    private Long courseId;

    private Long documentId;

    private Integer chunkIndex;

    private String content;

    private String contentHash;

    private Integer pageNumber;

    private String sectionTitle;

    private Integer tokenCount;

    private String vectorKey;

    private String vectorStatus;

    private String embeddingModel;

    private Integer embeddingDimension;

    private String fileName;

    private String fileType;

    private String documentType;

    private LocalDateTime createdAt;
}