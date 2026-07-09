package com.msc.springai.dto.learning.projection;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LowScoreQuizCandidate {

    private Long quizId;

    private String quizTitle;

    private Double score;

    private String topic;

    private LocalDateTime submittedAt;
}