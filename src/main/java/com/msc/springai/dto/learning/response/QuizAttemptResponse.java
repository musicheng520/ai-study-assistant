package com.msc.springai.dto.learning.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class QuizAttemptResponse {

    private Long attemptId;

    private Long quizId;

    private Double score;

    private Integer totalQuestions;

    private Integer correctCount;

    private LocalDateTime startedAt;

    private LocalDateTime submittedAt;
}