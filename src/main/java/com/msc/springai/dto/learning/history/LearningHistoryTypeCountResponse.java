package com.msc.springai.dto.learning.history;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LearningHistoryTypeCountResponse {

    private String eventType;

    private Long count;
}