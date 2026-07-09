package com.msc.springai.dto.learning.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReviewRecommendationItemResponse {

    private String type;

    private String topic;

    private Long quizId;

    private Long documentId;

    private String reason;

    private Integer priority;

    private String action;
}