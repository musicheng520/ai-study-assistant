package com.msc.springai.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

public final class CourseOverviewDtos {

    private CourseOverviewDtos() {
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CourseOverviewResponse {

        private Long courseId;

        private String courseName;

        private String courseCode;

        private String courseColor;

        private Double progressScore;

        private CourseOverviewStatsResponse stats;

        private List<WeakTopicItemResponse> weakTopics;

        private List<NextActionResponse> nextActions;

        private List<RecentActivityResponse> recentActivities;

        private LocalDateTime generatedAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CourseOverviewStatsResponse {

        private Long documentCount;

        private Long readyDocumentCount;

        private Long processingDocumentCount;

        private Long failedDocumentCount;

        private Long chatMessageCount;

        private Long summaryCount;

        private Long quizCount;

        private Long quizAttemptCount;

        private Double averageQuizScore;

        private Long wrongAnswerCount;

        private Long unresolvedWrongAnswerCount;

        private Long flashcardCount;

        private Long noteCount;

        private Long taskCount;

        private Long completedTaskCount;

        private Long revisionPackCount;

        private Long assignmentAnalysisCount;

        private Long rubricAnalysisCount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WeakTopicItemResponse {

        private String topic;

        private Long wrongCount;

        private Long unresolvedCount;

        private LocalDateTime latestWrongAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NextActionResponse {

        private String type;

        private String title;

        private String reason;

        private Integer priority;

        private String actionLabel;

        private String targetPath;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentActivityResponse {

        private Long id;

        private String eventType;

        private String targetType;

        private Long targetId;

        private String topic;

        private String title;

        private String iconType;

        private LocalDateTime createdAt;
    }

    /*
     * Mapper row.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CourseHeaderRow {

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
    public static class CourseOverviewStatsRow {

        private Long documentCount;

        private Long readyDocumentCount;

        private Long processingDocumentCount;

        private Long failedDocumentCount;

        private Long chatMessageCount;

        private Long summaryCount;

        private Long quizCount;

        private Long quizAttemptCount;

        private Double averageQuizScore;

        private Long wrongAnswerCount;

        private Long unresolvedWrongAnswerCount;

        private Long flashcardCount;

        private Long noteCount;

        private Long taskCount;

        private Long completedTaskCount;

        private Long revisionPackCount;

        private Long assignmentAnalysisCount;

        private Long rubricAnalysisCount;
    }

    /*
     * Mapper row.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentActivityRow {

        private Long id;

        private String eventType;

        private String targetType;

        private Long targetId;

        private String topic;

        private LocalDateTime createdAt;
    }
}