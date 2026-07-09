package com.msc.springai.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class QuizAttemptAnswer {

    private Long id;

    private Long attemptId;

    private Long questionId;

    private String userAnswer;

    private Boolean isCorrect;

    private LocalDateTime createdAt;
}