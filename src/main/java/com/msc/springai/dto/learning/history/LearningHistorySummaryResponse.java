package com.msc.springai.dto.learning.history;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LearningHistorySummaryResponse {

    private Long courseId;

    private Long totalActivities;

    private LocalDateTime latestActivityAt;

    private List<LearningHistoryTypeCountResponse> eventTypeCounts;
}