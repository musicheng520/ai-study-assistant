package com.msc.springai.dto.learning.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WrongAnswerItemResponse {

    private Long wrongAnswerId;

    private Long quizId;

    private Long questionId;

    private String topic;

    private String userAnswer;

    private String correctAnswer;

    private String explanation;

    private Boolean resolved;

    private LocalDateTime createdAt;
}