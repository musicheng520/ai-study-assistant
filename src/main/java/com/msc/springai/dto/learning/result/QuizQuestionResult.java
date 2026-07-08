package com.msc.springai.dto.learning.result;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QuizQuestionResult {

    private String questionType;

    private String questionText;

    private List<String> options = new ArrayList<>();

    private String correctAnswer;

    private String explanation;

    private String difficulty;

    private String topic;

    private Long sourceChunkId;
}