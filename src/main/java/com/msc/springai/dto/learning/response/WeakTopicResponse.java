package com.msc.springai.dto.learning.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WeakTopicResponse {

    private String topic;

    private Integer wrongCount;

    private Integer resolvedCount;

    private Integer unresolvedCount;

    private LocalDateTime lastWrongAt;

    private Integer relatedQuizCount;
}