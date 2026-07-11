package com.msc.springai.dto.learning.history;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LearningHistoryListResponse {

    private Long courseId;

    private Integer limit;

    private Integer offset;

    private Integer count;

    private List<LearningHistoryItemResponse> activities;
}