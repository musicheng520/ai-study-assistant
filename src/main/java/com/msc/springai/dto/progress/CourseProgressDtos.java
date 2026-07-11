package com.msc.springai.dto.progress;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

public final class CourseProgressDtos {

    private CourseProgressDtos() {
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CourseProgressResponse {

        private Long courseId;

        private String courseName;

        private String courseCode;

        private String courseColor;

        private Double engagementScore;

        private Double masteryScore;

        private Double documentReadinessRate;

        private Double taskCompletionRate;

        private Double wrongAnswerResolvedRate;

        private Double averageQuizScore;

        private String progressLevel;

        private String riskLevel;

        private ProgressStatsResponse stats;

        private List<WeakTopicProgressResponse> weakTopics;

        private List<ProgressRecommendationResponse> recommendations;

        private LocalDateTime generatedAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProgressStatsResponse {

        private Long documentCount;

        private Long readyDocumentCount;

        private Long processingDocumentCount;

        private Long summaryCount;

        private Long quizCount;

        private Long quizAttemptCount;

        private Long flashcardCount;

        private Long wrongAnswerCount;

        private Long unresolvedWrongAnswerCount;

        private Long resolvedWrongAnswerCount;

        private Long taskCount;

        private Long completedTaskCount;

        private Long revisionPackCount;

        private Long recentActivityCount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WeakTopicProgressResponse {

        private String topic;

        private Long wrongCount;

        private Long unresolvedCount;

        private Double unresolvedRate;

        private LocalDateTime latestWrongAt;

        private String severity;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProgressRecommendationListResponse {

        private Long courseId;

        private List<ProgressRecommendationResponse> recommendations;

        private LocalDateTime generatedAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProgressRecommendationResponse {

        private String type;

        private String title;

        private String reason;

        private Integer priority;

        private String actionLabel;

        private String targetPath;
    }

    /*
     * Mapper row.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CourseProgressHeaderRow {

        private Long courseId;

        private String courseName;

        private String courseCode;

        private String courseColor;

        private Double storedProgressScore;
    }

    /*
     * Mapper row.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CourseProgressStatsRow {

        private Long documentCount;

        private Long readyDocumentCount;

        private Long processingDocumentCount;

        private Long summaryCount;

        private Long quizCount;

        private Long quizAttemptCount;

        private Double averageQuizScore;

        private Long flashcardCount;

        private Long wrongAnswerCount;

        private Long unresolvedWrongAnswerCount;

        private Long resolvedWrongAnswerCount;

        private Long taskCount;

        private Long completedTaskCount;

        private Long revisionPackCount;

        private Long recentActivityCount;
    }

    /*
     * Mapper row.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WeakTopicProgressRow {

        private String topic;

        private Long wrongCount;

        private Long unresolvedCount;

        private LocalDateTime latestWrongAt;
    }
}