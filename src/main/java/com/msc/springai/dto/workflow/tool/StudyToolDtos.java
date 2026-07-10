package com.msc.springai.dto.workflow.tool;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class StudyToolDtos {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CourseDocumentToolResult {
        private Long documentId;
        private String originalFileName;
        private String documentType;
        private String status;
        private Integer chunkCount;
        private LocalDateTime processedAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LearningHistoryToolResult {
        private String eventType;
        private String targetType;
        private Long targetId;
        private String topic;
        private LocalDateTime createdAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WrongAnswerTopicSummary {
        private String topic;
        private Integer wrongCount;
        private Integer unresolvedCount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WrongAnswerItem {
        private Long wrongAnswerId;
        private Long quizId;
        private Long questionId;
        private String topic;
        private String userAnswer;
        private String correctAnswer;
        private String explanation;
        private Boolean resolved;
        private LocalDateTime createdAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WrongAnswersToolResult {
        private List<WrongAnswerTopicSummary> weakTopics;
        private List<WrongAnswerItem> wrongAnswerItems;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NoteToolResult {
        private Long noteId;
        private String title;
        private String content;
        private String topic;
        private Long documentId;
        private LocalDateTime createdAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CourseProgressToolResult {
        private BigDecimal progressScore;
        private BigDecimal averageQuizScore;
        private Integer wrongAnswerCount;
        private Integer unresolvedWrongAnswerCount;
        private List<WrongAnswerTopicSummary> weakTopics;
        private String recommendedNextReview;
        private List<LearningHistoryToolResult> recentActivity;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocumentSearchToolResult {
        private Long chunkId;
        private Long documentId;
        private String fileName;
        private Integer pageNumber;
        private String sectionTitle;
        private String content;
        private Double score;
    }
}