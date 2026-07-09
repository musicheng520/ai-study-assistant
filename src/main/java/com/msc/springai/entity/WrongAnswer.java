package com.msc.springai.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WrongAnswer {

    private Long id;

    private Long userId;

    private Long courseId;

    private Long quizId;

    private Long questionId;

    private String topic;

    private String userAnswer;

    private String correctAnswer;

    private String explanation;

    private Boolean resolved;

    private LocalDateTime createdAt;
}