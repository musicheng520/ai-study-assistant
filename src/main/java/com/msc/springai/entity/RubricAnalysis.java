package com.msc.springai.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RubricAnalysis {

    private Long id;

    private Long userId;

    private Long courseId;

    private Long documentId;

    private String criteriaJson;

    private String excellentBandJson;

    private String commonMistakes;

    private String highScoreStrategy;

    private LocalDateTime createdAt;
}