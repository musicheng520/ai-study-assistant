package com.msc.springai.dto.learning.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FlashcardGenerateRequest {

    private Integer topK;

    private String retrievalQuery;

    private Integer count;

    private String difficulty;
}