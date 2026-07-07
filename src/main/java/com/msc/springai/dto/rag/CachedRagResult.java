package com.msc.springai.dto.rag;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CachedRagResult {

    private String answer;

    private Boolean noAnswer;

    private String workflowType;

    private Integer retrievedChunkCount;

    private List<CitationResponse> citations;
}