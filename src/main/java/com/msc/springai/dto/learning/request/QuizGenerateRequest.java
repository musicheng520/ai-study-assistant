package com.msc.springai.dto.learning.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class QuizGenerateRequest {

    private Integer topK;

    private String retrievalQuery;

    private Integer mcqCount;

    private Integer shortAnswerCount;

    private String difficulty;
}