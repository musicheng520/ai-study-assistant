package com.msc.springai.dto.document;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DocumentStatusResponse {

    private Long documentId;
    private String status;
    private String errorMessage;
    private Integer chunkCount;
}