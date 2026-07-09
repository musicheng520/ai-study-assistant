package com.msc.springai.dto.learning.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CourseProgressResponse {

    private Long courseId;

    private Integer documentCount;

    private Integer readyDocumentCount;

    private Integer chatMessageCount;

    private Integer summaryCount;

    private Integer quizCount;

    private Integer quizAttemptCount;

    private Double averageQuizScore;

    private Integer wrongAnswerCount;

    private Integer unresolvedWrongAnswerCount;

    private Integer flashcardCount;

    private Integer noteCount;

    private Double progressScore;

    private List<WeakTopicResponse> weakTopics;

    private String recommendedNextReview;

    private List<RecentActivityResponse> recentActivity;
}