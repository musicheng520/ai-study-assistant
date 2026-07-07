package com.msc.springai.dto.rag;

import lombok.Data;

@Data
public class RagAskRequest {

    /**
     * User question.
     */
    private String question;

    /**
     * Optional.
     * If null, backend will create a new chat session.
     */
    private Long sessionId;

    /**
     * Optional.
     * Default should be 5 in service layer.
     */
    private Integer topK;
}