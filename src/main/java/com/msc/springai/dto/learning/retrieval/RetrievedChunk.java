package com.msc.springai.dto.learning.retrieval;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RetrievedChunk {

    private Long chunkId;

    private Long documentId;

    private String fileName;

    private Integer pageNumber;

    private String sectionTitle;

    private String content;

    private Double score;
}