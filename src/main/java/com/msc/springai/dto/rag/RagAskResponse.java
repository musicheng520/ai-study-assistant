package com.msc.springai.dto.rag;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RagAskResponse {

    private Long sessionId;

    private Long userMessageId;

    private Long assistantMessageId;

    private String answer;

    private Boolean noAnswer;

    private String workflowType;

    private Integer retrievedChunkCount;

    private List<CitationResponse> citations;
}