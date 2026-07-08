package com.msc.springai.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class QuizQuestion {

    private Long id;

    private Long quizId;

    private String questionType;

    private String questionText;

    private String optionsJson;

    private String correctAnswer;

    private String explanation;

    private String difficulty;

    private String topic;

    private Long sourceChunkId;

    private LocalDateTime createdAt;
}