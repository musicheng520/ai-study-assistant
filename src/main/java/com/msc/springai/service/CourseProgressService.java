package com.msc.springai.service;

import com.msc.springai.dto.progress.CourseProgressDtos.CourseProgressHeaderRow;
import com.msc.springai.dto.progress.CourseProgressDtos.CourseProgressResponse;
import com.msc.springai.dto.progress.CourseProgressDtos.CourseProgressStatsRow;
import com.msc.springai.dto.progress.CourseProgressDtos.ProgressRecommendationListResponse;
import com.msc.springai.dto.progress.CourseProgressDtos.ProgressRecommendationResponse;
import com.msc.springai.dto.progress.CourseProgressDtos.ProgressStatsResponse;
import com.msc.springai.dto.progress.CourseProgressDtos.WeakTopicProgressResponse;
import com.msc.springai.dto.progress.CourseProgressDtos.WeakTopicProgressRow;
import com.msc.springai.exception.BusinessException;
import com.msc.springai.mapper.CourseProgressMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CourseProgressService {

    private static final int WEAK_TOPIC_LIMIT = 6;

    private final CourseProgressMapper courseProgressMapper;

    public CourseProgressResponse getCourseProgress(
            Long currentUserId,
            Long courseId
    ) {
        validateCurrentUser(currentUserId);
        validateCourseId(courseId);

        CourseProgressHeaderRow course =
                courseProgressMapper.findCourseHeader(
                        currentUserId,
                        courseId
                );

        if (course == null) {
            throw new BusinessException(
                    "COURSE_NOT_FOUND",
                    "Course not found or access denied."
            );
        }

        CourseProgressStatsRow stats =
                courseProgressMapper.findProgressStats(
                        currentUserId,
                        courseId
                );

        if (stats == null) {
            stats = new CourseProgressStatsRow();
        }

        List<WeakTopicProgressResponse> weakTopics =
                courseProgressMapper.findWeakTopics(
                                currentUserId,
                                courseId,
                                WEAK_TOPIC_LIMIT
                        )
                        .stream()
                        .map(this::toWeakTopicResponse)
                        .toList();

        Double engagementScore =
                safeDouble(course.getStoredProgressScore());

        Double masteryScore =
                calculateMasteryScore(stats);

        Double documentReadinessRate =
                rate(
                        safeLong(stats.getReadyDocumentCount()),
                        safeLong(stats.getDocumentCount())
                );

        Double taskCompletionRate =
                rate(
                        safeLong(stats.getCompletedTaskCount()),
                        safeLong(stats.getTaskCount())
                );

        Double wrongAnswerResolvedRate =
                rate(
                        safeLong(stats.getResolvedWrongAnswerCount()),
                        safeLong(stats.getWrongAnswerCount())
                );

        List<ProgressRecommendationResponse> recommendations =
                buildRecommendations(
                        courseId,
                        stats,
                        weakTopics,
                        masteryScore,
                        documentReadinessRate,
                        taskCompletionRate,
                        wrongAnswerResolvedRate
                );

        return new CourseProgressResponse(
                course.getCourseId(),
                course.getCourseName(),
                course.getCourseCode(),
                course.getCourseColor(),
                round2(engagementScore),
                round2(masteryScore),
                round2(documentReadinessRate),
                round2(taskCompletionRate),
                round2(wrongAnswerResolvedRate),
                round2(safeDouble(stats.getAverageQuizScore())),
                buildProgressLevel(masteryScore),
                buildRiskLevel(stats, masteryScore),
                toStatsResponse(stats),
                weakTopics,
                recommendations,
                LocalDateTime.now()
        );
    }

    public ProgressRecommendationListResponse getRecommendations(
            Long currentUserId,
            Long courseId
    ) {
        CourseProgressResponse progress =
                getCourseProgress(
                        currentUserId,
                        courseId
                );

        return new ProgressRecommendationListResponse(
                courseId,
                progress.getRecommendations(),
                LocalDateTime.now()
        );
    }

    private ProgressStatsResponse toStatsResponse(
            CourseProgressStatsRow stats
    ) {
        return new ProgressStatsResponse(
                safeLong(stats.getDocumentCount()),
                safeLong(stats.getReadyDocumentCount()),
                safeLong(stats.getProcessingDocumentCount()),
                safeLong(stats.getSummaryCount()),
                safeLong(stats.getQuizCount()),
                safeLong(stats.getQuizAttemptCount()),
                safeLong(stats.getFlashcardCount()),
                safeLong(stats.getWrongAnswerCount()),
                safeLong(stats.getUnresolvedWrongAnswerCount()),
                safeLong(stats.getResolvedWrongAnswerCount()),
                safeLong(stats.getTaskCount()),
                safeLong(stats.getCompletedTaskCount()),
                safeLong(stats.getRevisionPackCount()),
                safeLong(stats.getRecentActivityCount())
        );
    }

    private WeakTopicProgressResponse toWeakTopicResponse(
            WeakTopicProgressRow row
    ) {
        long wrongCount =
                safeLong(row.getWrongCount());

        long unresolvedCount =
                safeLong(row.getUnresolvedCount());

        double unresolvedRate =
                rate(
                        unresolvedCount,
                        wrongCount
                );

        return new WeakTopicProgressResponse(
                row.getTopic(),
                wrongCount,
                unresolvedCount,
                round2(unresolvedRate),
                row.getLatestWrongAt(),
                buildWeakTopicSeverity(
                        unresolvedCount,
                        unresolvedRate
                )
        );
    }

    private Double calculateMasteryScore(
            CourseProgressStatsRow stats
    ) {
        /*
         * Mastery score 和 Overview 的 engagementScore 不一样。
         *
         * Mastery 更强调：
         * 1. Quiz average score
         * 2. Wrong answers resolved rate
         * 3. Task completion rate
         * 4. Revision activity
         *
         * 这样能避免“用户点了很多功能但掌握度不高”的情况。
         */
        double averageQuizScore =
                safeDouble(stats.getAverageQuizScore());

        double wrongResolvedRate =
                rate(
                        safeLong(stats.getResolvedWrongAnswerCount()),
                        safeLong(stats.getWrongAnswerCount())
                );

        double taskCompletionRate =
                rate(
                        safeLong(stats.getCompletedTaskCount()),
                        safeLong(stats.getTaskCount())
                );

        double revisionScore =
                safeLong(stats.getRevisionPackCount()) > 0
                        ? 100.0
                        : 0.0;

        double quizWeight =
                safeLong(stats.getQuizAttemptCount()) > 0
                        ? averageQuizScore
                        : 0.0;

        double score =
                quizWeight * 0.50
                        + wrongResolvedRate * 0.25
                        + taskCompletionRate * 0.15
                        + revisionScore * 0.10;

        return clampScore(
                score
        );
    }

    private List<ProgressRecommendationResponse> buildRecommendations(
            Long courseId,
            CourseProgressStatsRow stats,
            List<WeakTopicProgressResponse> weakTopics,
            Double masteryScore,
            Double documentReadinessRate,
            Double taskCompletionRate,
            Double wrongAnswerResolvedRate
    ) {
        List<ProgressRecommendationResponse> recommendations =
                new ArrayList<>();

        int priority = 1;

        if (safeLong(stats.getDocumentCount()) == 0) {
            recommendations.add(
                    new ProgressRecommendationResponse(
                            "UPLOAD_DOCUMENT",
                            "Upload your first course document",
                            "No course documents have been uploaded yet, so AI study features cannot use course evidence.",
                            priority++,
                            "Upload Document",
                            "/courses/" + courseId + "/documents"
                    )
            );
        } else if (documentReadinessRate < 100.0) {
            recommendations.add(
                    new ProgressRecommendationResponse(
                            "WAIT_FOR_PROCESSING",
                            "Wait for document processing to finish",
                            "Some uploaded documents are still processing. RAG and study generation will be stronger after processing is complete.",
                            priority++,
                            "View Documents",
                            "/courses/" + courseId + "/documents"
                    )
            );
        }

        WeakTopicProgressResponse topWeakTopic =
                firstHighPriorityWeakTopic(
                        weakTopics
                );

        if (topWeakTopic != null) {
            recommendations.add(
                    new ProgressRecommendationResponse(
                            "REVIEW_WEAK_TOPIC",
                            "Review weak topic: " + topWeakTopic.getTopic(),
                            "This topic has "
                                    + topWeakTopic.getUnresolvedCount()
                                    + " unresolved wrong answer(s).",
                            priority++,
                            "Open Revision",
                            "/courses/" + courseId + "/revision"
                    )
            );
        }

        if (safeLong(stats.getQuizCount()) > 0
                && safeLong(stats.getQuizAttemptCount()) == 0) {
            recommendations.add(
                    new ProgressRecommendationResponse(
                            "TAKE_QUIZ",
                            "Attempt a saved quiz",
                            "You already have saved quizzes, but there are no quiz attempts yet.",
                            priority++,
                            "Start Quiz",
                            "/courses/" + courseId + "/quizzes"
                    )
            );
        }

        if (safeLong(stats.getQuizAttemptCount()) > 0
                && safeDouble(stats.getAverageQuizScore()) < 60.0) {
            recommendations.add(
                    new ProgressRecommendationResponse(
                            "IMPROVE_QUIZ_SCORE",
                            "Improve quiz performance",
                            "Your average quiz score is below 60%, so reviewing explanations and weak topics should be the next priority.",
                            priority++,
                            "Review Wrong Answers",
                            "/courses/" + courseId + "/revision"
                    )
            );
        }

        if (safeLong(stats.getSummaryCount()) == 0
                && safeLong(stats.getReadyDocumentCount()) > 0) {
            recommendations.add(
                    new ProgressRecommendationResponse(
                            "GENERATE_SUMMARY",
                            "Generate a course summary",
                            "You have ready documents but no saved summary yet. A summary gives you structured revision notes.",
                            priority++,
                            "Generate Summary",
                            "/courses/" + courseId + "/study"
                    )
            );
        }

        if (safeLong(stats.getFlashcardCount()) == 0
                && safeLong(stats.getReadyDocumentCount()) > 0) {
            recommendations.add(
                    new ProgressRecommendationResponse(
                            "GENERATE_FLASHCARDS",
                            "Create flashcards for active recall",
                            "Flashcards help convert course material into quick revision questions.",
                            priority++,
                            "Generate Flashcards",
                            "/courses/" + courseId + "/flashcards"
                    )
            );
        }

        long incompleteTasks =
                Math.max(
                        safeLong(stats.getTaskCount())
                                - safeLong(stats.getCompletedTaskCount()),
                        0L
                );

        if (incompleteTasks > 0
                && taskCompletionRate < 80.0) {
            recommendations.add(
                    new ProgressRecommendationResponse(
                            "COMPLETE_TASKS",
                            "Complete pending study tasks",
                            "You still have "
                                    + incompleteTasks
                                    + " unfinished task(s).",
                            priority++,
                            "Open Tasks",
                            "/courses/" + courseId + "/assignment"
                    )
            );
        }

        if (safeLong(stats.getWrongAnswerCount()) > 0
                && wrongAnswerResolvedRate < 50.0) {
            recommendations.add(
                    new ProgressRecommendationResponse(
                            "RESOLVE_WRONG_ANSWERS",
                            "Resolve wrong answers",
                            "Less than half of your wrong answers have been resolved. Focused review will improve mastery.",
                            priority++,
                            "Open Wrong Answers",
                            "/courses/" + courseId + "/revision"
                    )
            );
        }

        if (safeLong(stats.getRevisionPackCount()) == 0
                && (
                safeLong(stats.getWrongAnswerCount()) > 0
                        || safeLong(stats.getQuizAttemptCount()) > 0
        )) {
            recommendations.add(
                    new ProgressRecommendationResponse(
                            "GENERATE_REVISION_PACK",
                            "Generate a revision pack",
                            "A revision pack can combine wrong answers, weak topics and course material into a focused study plan.",
                            priority++,
                            "Generate Revision Pack",
                            "/courses/" + courseId + "/revision"
                    )
            );
        }

        if (recommendations.isEmpty()) {
            recommendations.add(
                    new ProgressRecommendationResponse(
                            "KEEP_GOING",
                            "Keep your learning momentum",
                            "Your current progress looks stable. Continue reviewing, testing yourself and completing study tasks.",
                            priority,
                            "Continue Studying",
                            "/courses/" + courseId + "/overview"
                    )
            );
        }

        return recommendations.size() <= 6
                ? recommendations
                : recommendations.subList(0, 6);
    }

    private WeakTopicProgressResponse firstHighPriorityWeakTopic(
            List<WeakTopicProgressResponse> weakTopics
    ) {
        if (weakTopics == null || weakTopics.isEmpty()) {
            return null;
        }

        for (WeakTopicProgressResponse topic : weakTopics) {
            if (topic == null) {
                continue;
            }

            if (safeLong(topic.getUnresolvedCount()) > 0) {
                return topic;
            }
        }

        return null;
    }

    private String buildProgressLevel(
            Double masteryScore
    ) {
        double score =
                safeDouble(masteryScore);

        if (score >= 80.0) {
            return "STRONG";
        }

        if (score >= 60.0) {
            return "GOOD";
        }

        if (score >= 40.0) {
            return "NEEDS_WORK";
        }

        return "AT_RISK";
    }

    private String buildRiskLevel(
            CourseProgressStatsRow stats,
            Double masteryScore
    ) {
        double score =
                safeDouble(masteryScore);

        long unresolvedWrongAnswers =
                safeLong(
                        stats.getUnresolvedWrongAnswerCount()
                );

        double averageQuizScore =
                safeDouble(
                        stats.getAverageQuizScore()
                );

        if (score < 40.0
                || unresolvedWrongAnswers >= 10
                || (
                safeLong(stats.getQuizAttemptCount()) > 0
                        && averageQuizScore < 50.0
        )) {
            return "HIGH";
        }

        if (score < 65.0
                || unresolvedWrongAnswers >= 5
                || (
                safeLong(stats.getQuizAttemptCount()) > 0
                        && averageQuizScore < 65.0
        )) {
            return "MEDIUM";
        }

        return "LOW";
    }

    private String buildWeakTopicSeverity(
            long unresolvedCount,
            double unresolvedRate
    ) {
        if (unresolvedCount >= 5
                || unresolvedRate >= 80.0) {
            return "HIGH";
        }

        if (unresolvedCount >= 2
                || unresolvedRate >= 50.0) {
            return "MEDIUM";
        }

        return "LOW";
    }

    private void validateCurrentUser(
            Long currentUserId
    ) {
        if (currentUserId == null) {
            throw new BusinessException(
                    "UNAUTHORIZED",
                    "Current user is required."
            );
        }
    }

    private void validateCourseId(
            Long courseId
    ) {
        if (courseId == null) {
            throw new BusinessException(
                    "INVALID_COURSE_ID",
                    "Course id is required."
            );
        }
    }

    private long safeLong(
            Long value
    ) {
        return value == null
                ? 0L
                : Math.max(value, 0L);
    }

    private double safeDouble(
            Double value
    ) {
        if (value == null) {
            return 0.0;
        }

        if (Double.isNaN(value)
                || Double.isInfinite(value)) {
            return 0.0;
        }

        return Math.max(value, 0.0);
    }

    private double rate(
            long numerator,
            long denominator
    ) {
        if (denominator <= 0) {
            return 0.0;
        }

        return numerator * 100.0 / denominator;
    }

    private Double clampScore(
            double score
    ) {
        if (score < 0.0) {
            return 0.0;
        }

        if (score > 100.0) {
            return 100.0;
        }

        return round2(score);
    }

    private double round2(
            double value
    ) {
        return Math.round(value * 100.0) / 100.0;
    }
}