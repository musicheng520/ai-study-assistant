package com.msc.springai.dto.learning.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserProgressOverviewResponse {

    private Integer courseCount;

    private Integer documentCount;

    private Integer readyDocumentCount;

    private Integer questionAskedCount;

    private Integer summaryCount;

    private Integer quizCount;

    private Integer flashcardCount;

    private Double averageQuizScore;

    private Integer currentStreak;

    private Integer longestStreak;

    private List<RecentActivityResponse> recentActivity;
}