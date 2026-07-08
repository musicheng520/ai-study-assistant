package com.msc.springai.dto.learning.response;

import com.msc.springai.dto.learning.result.QuizQuestionResult;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class QuizGenerateResponse {

    private String draftKey;

    private String title;

    private String difficulty;

    private String sourceScope;

    private Integer questionCount;

    private List<QuizQuestionResult> questions;
}