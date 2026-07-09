package com.msc.springai.service;

import com.msc.springai.dto.learning.response.*;
import com.msc.springai.entity.Course;
import com.msc.springai.entity.StudyStreak;
import com.msc.springai.exception.BusinessException;
import com.msc.springai.mapper.CourseMapper;
import com.msc.springai.mapper.ProgressStatsMapper;
import com.msc.springai.mapper.StudyStreakMapper;
import com.msc.springai.mapper.WrongAnswerMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.msc.springai.dto.learning.projection.DocumentReviewCandidate;
import com.msc.springai.dto.learning.projection.LowScoreQuizCandidate;
import com.msc.springai.mapper.ProgressRecommendationMapper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProgressService {

    private static final int RECENT_ACTIVITY_LIMIT = 10;
    private static final double LOW_SCORE_THRESHOLD = 60.0;
    private static final int REVIEW_STALE_DAYS = 7;
    private static final int MAX_RECOMMENDATIONS = 3;

    private final ProgressRecommendationMapper progressRecommendationMapper;

    private final CourseMapper courseMapper;
    private final WrongAnswerMapper wrongAnswerMapper;
    private final ProgressStatsMapper progressStatsMapper;
    private final StudyStreakMapper studyStreakMapper;

    public CourseWeakTopicsResponse getCourseWeakTopics(
            Long userId,
            Long courseId
    ) {
        validateCourseAccess(
                userId,
                courseId
        );

        List<WeakTopicResponse> weakTopics = wrongAnswerMapper.findWeakTopics(
                userId,
                courseId
        );

        return new CourseWeakTopicsResponse(
                courseId,
                weakTopics.size(),
                weakTopics
        );
    }

    public UserProgressOverviewResponse getUserOverview(Long userId) {
        StudyStreak streak = studyStreakMapper.findByUserId(userId);

        List<RecentActivityResponse> recentActivity =
                progressStatsMapper.findRecentActivityByUser(
                        userId,
                        RECENT_ACTIVITY_LIMIT
                );

        return new UserProgressOverviewResponse(
                safeInt(progressStatsMapper.countCourses(userId)),
                safeInt(progressStatsMapper.countDocuments(userId)),
                safeInt(progressStatsMapper.countReadyDocuments(userId)),
                safeInt(progressStatsMapper.countQuestionsAsked(userId)),
                safeInt(progressStatsMapper.countSummaries(userId)),
                safeInt(progressStatsMapper.countQuizzes(userId)),
                safeInt(progressStatsMapper.countFlashcards(userId)),
                safeDouble(progressStatsMapper.averageQuizScore(userId)),
                streak == null ? 0 : safeInt(streak.getCurrentStreak()),
                streak == null ? 0 : safeInt(streak.getLongestStreak()),
                recentActivity
        );
    }

    public CourseProgressResponse getCourseProgress(
            Long userId,
            Long courseId
    ) {
        validateCourseAccess(
                userId,
                courseId
        );

        int documentCount = safeInt(
                progressStatsMapper.countCourseDocuments(
                        userId,
                        courseId
                )
        );

        int readyDocumentCount = safeInt(
                progressStatsMapper.countCourseReadyDocuments(
                        userId,
                        courseId
                )
        );

        int chatMessageCount = safeInt(
                progressStatsMapper.countCourseChatMessages(
                        userId,
                        courseId
                )
        );

        int summaryCount = safeInt(
                progressStatsMapper.countCourseSummaries(
                        userId,
                        courseId
                )
        );

        int quizCount = safeInt(
                progressStatsMapper.countCourseQuizzes(
                        userId,
                        courseId
                )
        );

        int quizAttemptCount = safeInt(
                progressStatsMapper.countCourseQuizAttempts(
                        userId,
                        courseId
                )
        );

        double averageQuizScore = safeDouble(
                progressStatsMapper.averageCourseQuizScore(
                        userId,
                        courseId
                )
        );

        int wrongAnswerCount = safeInt(
                progressStatsMapper.countCourseWrongAnswers(
                        userId,
                        courseId
                )
        );

        int unresolvedWrongAnswerCount = safeInt(
                progressStatsMapper.countCourseUnresolvedWrongAnswers(
                        userId,
                        courseId
                )
        );

        int flashcardCount = safeInt(
                progressStatsMapper.countCourseFlashcards(
                        userId,
                        courseId
                )
        );

        /*
         * notes 模块还没有正式做，所以 Day 4 这里先返回 0。
         * 如果后面实现 notes 表和接口，再把这里替换成 countCourseNotes。
         */
        int noteCount = 0;

        double progressScore = calculateProgressScore(
                readyDocumentCount,
                summaryCount,
                quizAttemptCount,
                flashcardCount
        );

        progressStatsMapper.updateCourseProgressScore(
                userId,
                courseId,
                progressScore
        );

        List<WeakTopicResponse> weakTopics = wrongAnswerMapper.findWeakTopics(
                userId,
                courseId
        );

        List<RecentActivityResponse> recentActivity =
                progressStatsMapper.findRecentActivityByCourse(
                        userId,
                        courseId,
                        RECENT_ACTIVITY_LIMIT
                );

        return new CourseProgressResponse(
                courseId,
                documentCount,
                readyDocumentCount,
                chatMessageCount,
                summaryCount,
                quizCount,
                quizAttemptCount,
                averageQuizScore,
                wrongAnswerCount,
                unresolvedWrongAnswerCount,
                flashcardCount,
                noteCount,
                progressScore,
                weakTopics,
                null,
                recentActivity
        );
    }

    public CourseReviewRecommendationsResponse getCourseRecommendations(
            Long userId,
            Long courseId
    ) {
        validateCourseAccess(
                userId,
                courseId
        );

        List<ReviewRecommendationItemResponse> recommendations = new ArrayList<>();
        Set<String> addedTopics = new HashSet<>();

        List<WeakTopicResponse> weakTopics = wrongAnswerMapper.findWeakTopics(
                userId,
                courseId
        );

        int priority = 1;

        priority = addWeakTopicRecommendation(
                recommendations,
                addedTopics,
                weakTopics,
                priority
        );

        priority = addStaleReviewTopicRecommendation(
                userId,
                courseId,
                recommendations,
                addedTopics,
                weakTopics,
                priority
        );

        priority = addLowScoreQuizRecommendation(
                userId,
                courseId,
                recommendations,
                addedTopics,
                priority
        );

        if (recommendations.isEmpty()) {
            addDocumentPracticeRecommendation(
                    userId,
                    courseId,
                    recommendations,
                    priority
            );
        }

        return new CourseReviewRecommendationsResponse(
                courseId,
                recommendations.size(),
                recommendations
        );
    }

    private int addWeakTopicRecommendation(
            List<ReviewRecommendationItemResponse> recommendations,
            Set<String> addedTopics,
            List<WeakTopicResponse> weakTopics,
            int priority
    ) {
        for (WeakTopicResponse weakTopic : weakTopics) {
            if (safeInt(weakTopic.getUnresolvedCount()) <= 0) {
                continue;
            }

            String topic = normalizeTopic(weakTopic.getTopic());

            recommendations.add(new ReviewRecommendationItemResponse(
                    "WEAK_TOPIC",
                    topic,
                    null,
                    null,
                    "You have " + weakTopic.getUnresolvedCount()
                            + " unresolved wrong answers in this topic.",
                    priority,
                    "Review this topic and generate flashcards."
            ));

            addedTopics.add(topic.toLowerCase());

            return priority + 1;
        }

        return priority;
    }

    private int addStaleReviewTopicRecommendation(
            Long userId,
            Long courseId,
            List<ReviewRecommendationItemResponse> recommendations,
            Set<String> addedTopics,
            List<WeakTopicResponse> weakTopics,
            int priority
    ) {
        if (recommendations.size() >= MAX_RECOMMENDATIONS) {
            return priority;
        }

        LocalDateTime since = LocalDateTime.now().minusDays(REVIEW_STALE_DAYS);

        for (WeakTopicResponse weakTopic : weakTopics) {
            if (recommendations.size() >= MAX_RECOMMENDATIONS) {
                return priority;
            }

            if (safeInt(weakTopic.getUnresolvedCount()) <= 0) {
                continue;
            }

            String topic = normalizeTopic(weakTopic.getTopic());

            if (addedTopics.contains(topic.toLowerCase())) {
                continue;
            }

            Integer reviewCount = progressRecommendationMapper.countTopicReviewsSince(
                    userId,
                    courseId,
                    topic,
                    since
            );

            if (safeInt(reviewCount) > 0) {
                continue;
            }

            recommendations.add(new ReviewRecommendationItemResponse(
                    "STALE_REVIEW_TOPIC",
                    topic,
                    null,
                    null,
                    "You have not reviewed this weak topic in the last "
                            + REVIEW_STALE_DAYS + " days.",
                    priority,
                    "Review this topic before taking another quiz."
            ));

            addedTopics.add(topic.toLowerCase());
            priority++;
        }

        return priority;
    }

    private int addLowScoreQuizRecommendation(
            Long userId,
            Long courseId,
            List<ReviewRecommendationItemResponse> recommendations,
            Set<String> addedTopics,
            int priority
    ) {
        if (recommendations.size() >= MAX_RECOMMENDATIONS) {
            return priority;
        }

        LowScoreQuizCandidate lowScoreQuiz =
                progressRecommendationMapper.findLatestLowScoreQuiz(
                        userId,
                        courseId,
                        LOW_SCORE_THRESHOLD
                );

        if (lowScoreQuiz == null) {
            return priority;
        }

        String topic = normalizeTopic(
                lowScoreQuiz.getTopic() == null || lowScoreQuiz.getTopic().isBlank()
                        ? lowScoreQuiz.getQuizTitle()
                        : lowScoreQuiz.getTopic()
        );

        if (addedTopics.contains(topic.toLowerCase())) {
            return priority;
        }

        recommendations.add(new ReviewRecommendationItemResponse(
                "LOW_SCORE_QUIZ",
                topic,
                lowScoreQuiz.getQuizId(),
                null,
                "Your latest low-score quiz for this topic was "
                        + safeDouble(lowScoreQuiz.getScore()) + "%.",
                priority,
                "Retake the quiz or review the related notes."
        ));

        addedTopics.add(topic.toLowerCase());

        return priority + 1;
    }

    private void addDocumentPracticeRecommendation(
            Long userId,
            Long courseId,
            List<ReviewRecommendationItemResponse> recommendations,
            int priority
    ) {
        DocumentReviewCandidate document =
                progressRecommendationMapper.findRecentReadyDocumentWithoutQuizAttempt(
                        userId,
                        courseId
                );

        if (document == null) {
            return;
        }

        recommendations.add(new ReviewRecommendationItemResponse(
                "DOCUMENT_NEEDS_PRACTICE",
                null,
                null,
                document.getDocumentId(),
                "The document \"" + document.getFileName()
                        + "\" is ready but has no quiz attempt yet.",
                priority,
                "Generate a quiz for this document and submit your first attempt."
        ));
    }

    public CourseActivityResponse getCourseActivity(
            Long userId,
            Long courseId
    ) {
        validateCourseAccess(
                userId,
                courseId
        );

        List<RecentActivityResponse> activities =
                progressStatsMapper.findRecentActivityByCourse(
                        userId,
                        courseId,
                        RECENT_ACTIVITY_LIMIT
                );

        return new CourseActivityResponse(
                courseId,
                activities.size(),
                activities
        );
    }

    public StudyStreakResponse getStudyStreak(Long userId) {
        StudyStreak streak = studyStreakMapper.findByUserId(userId);

        if (streak == null) {
            return new StudyStreakResponse(
                    0,
                    0,
                    null
            );
        }

        return new StudyStreakResponse(
                safeInt(streak.getCurrentStreak()),
                safeInt(streak.getLongestStreak()),
                streak.getLastActivityDate()
        );
    }

    private String normalizeTopic(String topic) {
        if (topic == null || topic.isBlank()) {
            return "General";
        }

        return topic.trim();
    }

    private double calculateProgressScore(
            int readyDocumentCount,
            int summaryCount,
            int quizAttemptCount,
            int flashcardCount
    ) {
        double score =
                readyDocumentCount * 10.0
                        + summaryCount * 5.0
                        + quizAttemptCount * 10.0
                        + flashcardCount * 1.0;

        return Math.min(
                100.0,
                Math.round(score * 100.0) / 100.0
        );
    }

    private void validateCourseAccess(
            Long userId,
            Long courseId
    ) {
        Course course = courseMapper.findByIdAndUserId(
                courseId,
                userId
        );

        if (course == null) {
            throw new BusinessException(
                    "COURSE_ACCESS_DENIED",
                    "Course not found or access denied."
            );
        }
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private double safeDouble(Double value) {
        if (value == null) {
            return 0.0;
        }

        return Math.round(value * 100.0) / 100.0;
    }
}