package com.msc.springai.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class QuizAttempt {

    private Long id;

    private Long userId;

    private Long courseId;

    private Long quizId;

    private Double score;

    private Integer totalQuestions;

    private Integer correctCount;

    private LocalDateTime startedAt;

    private LocalDateTime submittedAt;
}