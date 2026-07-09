package com.msc.springai.service;

import com.msc.springai.dto.learning.response.CourseProgressResponse;
import com.msc.springai.dto.learning.response.CourseWeakTopicsResponse;
import com.msc.springai.dto.learning.response.RecentActivityResponse;
import com.msc.springai.dto.learning.response.UserProgressOverviewResponse;
import com.msc.springai.dto.learning.response.WeakTopicResponse;
import com.msc.springai.entity.Course;
import com.msc.springai.entity.StudyStreak;
import com.msc.springai.exception.BusinessException;
import com.msc.springai.mapper.CourseMapper;
import com.msc.springai.mapper.ProgressStatsMapper;
import com.msc.springai.mapper.StudyStreakMapper;
import com.msc.springai.mapper.WrongAnswerMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProgressService {

    private static final int RECENT_ACTIVITY_LIMIT = 10;

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