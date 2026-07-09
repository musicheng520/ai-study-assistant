package com.msc.springai.dto.learning.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WrongAnswerResponse {

    private Long questionId;

    private String topic;

    private String userAnswer;

    private String correctAnswer;

    private String explanation;
}