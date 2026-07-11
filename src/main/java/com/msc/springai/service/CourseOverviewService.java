package com.msc.springai.service;

import com.msc.springai.constant.LearningEventTypes;
import com.msc.springai.dto.dashboard.CourseOverviewDtos.CourseHeaderRow;
import com.msc.springai.dto.dashboard.CourseOverviewDtos.CourseOverviewResponse;
import com.msc.springai.dto.dashboard.CourseOverviewDtos.CourseOverviewStatsResponse;
import com.msc.springai.dto.dashboard.CourseOverviewDtos.CourseOverviewStatsRow;
import com.msc.springai.dto.dashboard.CourseOverviewDtos.NextActionResponse;
import com.msc.springai.dto.dashboard.CourseOverviewDtos.RecentActivityResponse;
import com.msc.springai.dto.dashboard.CourseOverviewDtos.RecentActivityRow;
import com.msc.springai.dto.dashboard.CourseOverviewDtos.WeakTopicItemResponse;
import com.msc.springai.exception.BusinessException;
import com.msc.springai.mapper.CourseOverviewMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CourseOverviewService {

    private static final int WEAK_TOPIC_LIMIT = 5;

    private static final int RECENT_ACTIVITY_LIMIT = 8;

    private final CourseOverviewMapper courseOverviewMapper;

    @Transactional
    public CourseOverviewResponse getCourseOverview(
            Long currentUserId,
            Long courseId
    ) {
        validateCurrentUser(currentUserId);
        validateCourseId(courseId);

        CourseHeaderRow course =
                courseOverviewMapper.findCourseHeader(
                        currentUserId,
                        courseId
                );

        if (course == null) {
            throw new BusinessException(
                    "COURSE_NOT_FOUND",
                    "Course not found or access denied."
            );
        }

        CourseOverviewStatsRow statsRow =
                courseOverviewMapper.findCourseStats(
                        currentUserId,
                        courseId
                );

        if (statsRow == null) {
            statsRow = new CourseOverviewStatsRow();
        }

        Double progressScore =
                calculateProgressScore(statsRow);

        courseOverviewMapper.updateCourseProgressScore(
                currentUserId,
                courseId,
                progressScore
        );

        List<WeakTopicItemResponse> weakTopics =
                courseOverviewMapper.findWeakTopics(
                        currentUserId,
                        courseId,
                        WEAK_TOPIC_LIMIT
                );

        List<RecentActivityResponse> recentActivities =
                courseOverviewMapper
                        .findRecentActivities(
                                currentUserId,
                                courseId,
                                RECENT_ACTIVITY_LIMIT
                        )
                        .stream()
                        .map(this::toRecentActivityResponse)
                        .toList();

        List<NextActionResponse> nextActions =
                buildNextActions(
                        courseId,
                        statsRow,
                        weakTopics
                );

        return new CourseOverviewResponse(
                course.getCourseId(),
                course.getCourseName(),
                course.getCourseCode(),
                course.getCourseColor(),
                progressScore,
                toStatsResponse(statsRow),
                weakTopics,
                nextActions,
                recentActivities,
                LocalDateTime.now()
        );
    }

    private CourseOverviewStatsResponse toStatsResponse(
            CourseOverviewStatsRow row
    ) {
        return new CourseOverviewStatsResponse(
                safeLong(row.getDocumentCount()),
                safeLong(row.getReadyDocumentCount()),
                safeLong(row.getProcessingDocumentCount()),
                safeLong(row.getFailedDocumentCount()),
                safeLong(row.getChatMessageCount()),
                safeLong(row.getSummaryCount()),
                safeLong(row.getQuizCount()),
                safeLong(row.getQuizAttemptCount()),
                round2(safeDouble(row.getAverageQuizScore())),
                safeLong(row.getWrongAnswerCount()),
                safeLong(row.getUnresolvedWrongAnswerCount()),
                safeLong(row.getFlashcardCount()),
                safeLong(row.getNoteCount()),
                safeLong(row.getTaskCount()),
                safeLong(row.getCompletedTaskCount()),
                safeLong(row.getRevisionPackCount()),
                safeLong(row.getAssignmentAnalysisCount()),
                safeLong(row.getRubricAnalysisCount())
        );
    }

    private Double calculateProgressScore(
            CourseOverviewStatsRow row
    ) {
        /*
         * 简单、可解释、适合 Demo 的 progress 规则。
         *
         * ready document       每个 10 分，最多 30
         * quiz attempt         每次 10 分，最多 20
         * average quiz score   按 20% 加权，最多 20
         * summary              每个 5 分，最多 15
         * flashcard            每张 1 分，最多 10
         * completed task       每个 3 分，最多 15
         * revision pack        每个 8 分，最多 10
         *
         * 最后限制在 0 - 100。
         */
        double score = 0.0;

        score += Math.min(
                safeLong(row.getReadyDocumentCount()) * 10.0,
                30.0
        );

        score += Math.min(
                safeLong(row.getQuizAttemptCount()) * 10.0,
                20.0
        );

        score += Math.min(
                safeDouble(row.getAverageQuizScore()) * 0.2,
                20.0
        );

        score += Math.min(
                safeLong(row.getSummaryCount()) * 5.0,
                15.0
        );

        score += Math.min(
                safeLong(row.getFlashcardCount()) * 1.0,
                10.0
        );

        score += Math.min(
                safeLong(row.getCompletedTaskCount()) * 3.0,
                15.0
        );

        score += Math.min(
                safeLong(row.getRevisionPackCount()) * 8.0,
                10.0
        );

        score = Math.min(
                score,
                100.0
        );

        return round2(score);
    }

    private List<NextActionResponse> buildNextActions(
            Long courseId,
            CourseOverviewStatsRow stats,
            List<WeakTopicItemResponse> weakTopics
    ) {
        List<NextActionResponse> actions =
                new ArrayList<>();

        int priority = 1;

        if (safeLong(stats.getReadyDocumentCount()) == 0) {
            actions.add(
                    new NextActionResponse(
                            "UPLOAD_DOCUMENT",
                            "Upload or process course documents",
                            "This course has no ready documents yet, so AI features cannot use reliable course evidence.",
                            priority++,
                            "Go to Documents",
                            "/courses/" + courseId + "/documents"
                    )
            );
        }

        WeakTopicItemResponse topWeakTopic =
                firstUnresolvedWeakTopic(weakTopics);

        if (topWeakTopic != null) {
            actions.add(
                    new NextActionResponse(
                            "REVIEW_WEAK_TOPIC",
                            "Review weak topic: " + topWeakTopic.getTopic(),
                            "You have "
                                    + topWeakTopic.getUnresolvedCount()
                                    + " unresolved wrong answer(s) in this topic.",
                            priority++,
                            "Open Revision",
                            "/courses/" + courseId + "/revision"
                    )
            );
        }

        if (safeLong(stats.getSummaryCount()) == 0
                && safeLong(stats.getReadyDocumentCount()) > 0) {
            actions.add(
                    new NextActionResponse(
                            "GENERATE_SUMMARY",
                            "Generate your first summary",
                            "A summary helps turn uploaded documents into structured revision notes.",
                            priority++,
                            "Generate Summary",
                            "/courses/" + courseId + "/study"
                    )
            );
        }

        if (safeLong(stats.getQuizAttemptCount()) == 0
                && safeLong(stats.getQuizCount()) > 0) {
            actions.add(
                    new NextActionResponse(
                            "TAKE_QUIZ",
                            "Attempt a saved quiz",
                            "You have saved quizzes but no quiz attempts yet.",
                            priority++,
                            "Start Quiz",
                            "/courses/" + courseId + "/quizzes"
                    )
            );
        }

        long incompleteTasks =
                Math.max(
                        safeLong(stats.getTaskCount())
                                - safeLong(stats.getCompletedTaskCount()),
                        0L
                );

        if (incompleteTasks > 0) {
            actions.add(
                    new NextActionResponse(
                            "COMPLETE_TASKS",
                            "Complete study checklist tasks",
                            "You still have "
                                    + incompleteTasks
                                    + " task(s) waiting in this course.",
                            priority++,
                            "Open Tasks",
                            "/courses/" + courseId + "/assignment"
                    )
            );
        }

        if (safeLong(stats.getRevisionPackCount()) == 0
                && (
                safeLong(stats.getWrongAnswerCount()) > 0
                        || safeLong(stats.getQuizAttemptCount()) > 0
                        || safeLong(stats.getReadyDocumentCount()) > 0
        )) {
            actions.add(
                    new NextActionResponse(
                            "GENERATE_REVISION_PACK",
                            "Generate a revision pack",
                            "A revision pack can combine weak topics, documents, tasks and recent activity into a focused plan.",
                            priority,
                            "Generate Revision Pack",
                            "/courses/" + courseId + "/revision"
                    )
            );
        }

        return actions.size() <= 5
                ? actions
                : actions.subList(0, 5);
    }

    private WeakTopicItemResponse firstUnresolvedWeakTopic(
            List<WeakTopicItemResponse> weakTopics
    ) {
        if (weakTopics == null || weakTopics.isEmpty()) {
            return null;
        }

        for (WeakTopicItemResponse topic : weakTopics) {
            if (topic == null) {
                continue;
            }

            if (safeLong(topic.getUnresolvedCount()) > 0) {
                return topic;
            }
        }

        return null;
    }

    private RecentActivityResponse toRecentActivityResponse(
            RecentActivityRow row
    ) {
        String title =
                buildActivityTitle(
                        row.getEventType(),
                        row.getTopic()
                );

        String iconType =
                buildIconType(
                        row.getEventType()
                );

        return new RecentActivityResponse(
                row.getId(),
                row.getEventType(),
                row.getTargetType(),
                row.getTargetId(),
                row.getTopic(),
                title,
                iconType,
                row.getCreatedAt()
        );
    }

    private String buildActivityTitle(
            String eventType,
            String topic
    ) {
        if (LearningEventTypes.TASK_COMPLETED.equals(eventType)) {
            return topic == null || topic.isBlank()
                    ? "Completed a study task"
                    : "Completed task: " + topic;
        }

        if (LearningEventTypes.TASK_CREATED.equals(eventType)) {
            return topic == null || topic.isBlank()
                    ? "Created a study task"
                    : "Created task: " + topic;
        }

        if (LearningEventTypes.CHECKLIST.equals(eventType)) {
            return "Generated a study checklist";
        }

        if (LearningEventTypes.SUMMARY.equals(eventType)) {
            return topic == null || topic.isBlank()
                    ? "Generated a summary"
                    : "Generated summary: " + topic;
        }

        if (LearningEventTypes.QUIZ.equals(eventType)) {
            return topic == null || topic.isBlank()
                    ? "Saved a quiz"
                    : "Saved quiz: " + topic;
        }

        if (LearningEventTypes.FLASHCARD.equals(eventType)) {
            return topic == null || topic.isBlank()
                    ? "Saved flashcards"
                    : "Saved flashcards: " + topic;
        }

        if (LearningEventTypes.ASSIGNMENT_ANALYSIS.equals(eventType)) {
            return "Analyzed an assignment brief";
        }

        if (LearningEventTypes.RUBRIC_ANALYSIS.equals(eventType)) {
            return "Analyzed a marking rubric";
        }

        if (LearningEventTypes.REVISION_PACK.equals(eventType)) {
            return topic == null || topic.isBlank()
                    ? "Generated a revision pack"
                    : "Generated revision pack: " + topic;
        }

        if (LearningEventTypes.ASK.equals(eventType)) {
            return "Asked a course question";
        }

        return topic == null || topic.isBlank()
                ? "Learning activity"
                : topic;
    }

    private String buildIconType(
            String eventType
    ) {
        if (LearningEventTypes.TASK_COMPLETED.equals(eventType)) {
            return "TASK_DONE";
        }

        if (LearningEventTypes.TASK_CREATED.equals(eventType)) {
            return "TASK";
        }

        if (LearningEventTypes.CHECKLIST.equals(eventType)) {
            return "CHECKLIST";
        }

        if (LearningEventTypes.SUMMARY.equals(eventType)) {
            return "SUMMARY";
        }

        if (LearningEventTypes.QUIZ.equals(eventType)) {
            return "QUIZ";
        }

        if (LearningEventTypes.FLASHCARD.equals(eventType)) {
            return "FLASHCARD";
        }

        if (LearningEventTypes.ASSIGNMENT_ANALYSIS.equals(eventType)) {
            return "ASSIGNMENT";
        }

        if (LearningEventTypes.RUBRIC_ANALYSIS.equals(eventType)) {
            return "RUBRIC";
        }

        if (LearningEventTypes.REVISION_PACK.equals(eventType)) {
            return "REVISION";
        }

        if (LearningEventTypes.ASK.equals(eventType)) {
            return "CHAT";
        }

        return "ACTIVITY";
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

    private double round2(
            double value
    ) {
        return Math.round(value * 100.0) / 100.0;
    }
}