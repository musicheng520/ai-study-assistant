package com.msc.springai.dto.rag;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CitationResponse {

    private Integer citationIndex;

    private Long documentId;

    private Long chunkId;

    private String fileName;

    private Integer pageNumber;

    private String sectionTitle;

    private String snippet;

    private Double distance;
}