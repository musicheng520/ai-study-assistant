package com.msc.springai.service;

import com.msc.springai.constant.LearningEventTypes;
import com.msc.springai.constant.LearningTargetTypes;
import com.msc.springai.dto.learning.history.LearningHistoryItemResponse;
import com.msc.springai.dto.learning.history.LearningHistoryListResponse;
import com.msc.springai.dto.learning.history.LearningHistorySummaryResponse;
import com.msc.springai.dto.learning.history.LearningHistoryTypeCountResponse;
import com.msc.springai.entity.LearningHistory;
import com.msc.springai.exception.BusinessException;
import com.msc.springai.mapper.LearningHistoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LearningHistoryService {

    private static final int DEFAULT_LIMIT = 20;

    private static final int MAX_LIMIT = 100;

    private final LearningHistoryMapper learningHistoryMapper;

    public LearningHistoryListResponse getRecentActivities(
            Long currentUserId,
            Integer limit,
            Integer offset
    ) {
        validateCurrentUser(currentUserId);

        int normalizedLimit =
                normalizeLimit(limit);

        int normalizedOffset =
                normalizeOffset(offset);

        List<LearningHistoryItemResponse> activities =
                learningHistoryMapper
                        .findRecentByUserId(
                                currentUserId,
                                normalizedLimit,
                                normalizedOffset
                        )
                        .stream()
                        .map(this::toItemResponse)
                        .toList();

        return new LearningHistoryListResponse(
                null,
                normalizedLimit,
                normalizedOffset,
                activities.size(),
                activities
        );
    }

    public LearningHistoryListResponse getCourseActivities(
            Long currentUserId,
            Long courseId,
            Integer limit,
            Integer offset
    ) {
        validateCurrentUser(currentUserId);
        validateCourseId(courseId);

        ensureCourseAccess(
                currentUserId,
                courseId
        );

        int normalizedLimit =
                normalizeLimit(limit);

        int normalizedOffset =
                normalizeOffset(offset);

        List<LearningHistoryItemResponse> activities =
                learningHistoryMapper
                        .findByUserIdAndCourseId(
                                currentUserId,
                                courseId,
                                normalizedLimit,
                                normalizedOffset
                        )
                        .stream()
                        .map(this::toItemResponse)
                        .toList();

        return new LearningHistoryListResponse(
                courseId,
                normalizedLimit,
                normalizedOffset,
                activities.size(),
                activities
        );
    }

    public LearningHistorySummaryResponse getCourseActivitySummary(
            Long currentUserId,
            Long courseId
    ) {
        validateCurrentUser(currentUserId);
        validateCourseId(courseId);

        ensureCourseAccess(
                currentUserId,
                courseId
        );

        Long totalActivities =
                learningHistoryMapper.countByUserIdAndCourseId(
                        currentUserId,
                        courseId
                );

        LocalDateTime latestActivityAt =
                learningHistoryMapper.findLatestActivityAt(
                        currentUserId,
                        courseId
                );

        List<LearningHistoryTypeCountResponse> eventTypeCounts =
                learningHistoryMapper.countByEventType(
                        currentUserId,
                        courseId
                );

        return new LearningHistorySummaryResponse(
                courseId,
                safeLong(totalActivities),
                latestActivityAt,
                eventTypeCounts
        );
    }

    private LearningHistoryItemResponse toItemResponse(
            LearningHistory history
    ) {
        String eventType =
                history.getEventType();

        String targetType =
                history.getTargetType();

        String topic =
                history.getTopic();

        return new LearningHistoryItemResponse(
                history.getId(),
                history.getCourseId(),
                eventType,
                targetType,
                history.getTargetId(),
                topic,
                buildTitle(eventType, targetType, topic),
                buildDescription(eventType, targetType, topic),
                buildIconType(eventType, targetType),
                history.getCreatedAt()
        );
    }

    private String buildTitle(
            String eventType,
            String targetType,
            String topic
    ) {
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

        if (LearningEventTypes.ASK.equals(eventType)) {
            return "Asked a course question";
        }

        if (LearningEventTypes.DOCUMENT_UPLOAD.equals(eventType)) {
            return topic == null || topic.isBlank()
                    ? "Uploaded a document"
                    : "Uploaded document: " + topic;
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

        if (LearningEventTypes.CHECKLIST.equals(eventType)) {
            return "Generated a study checklist";
        }

        if (LearningEventTypes.TASK_CREATED.equals(eventType)) {
            return topic == null || topic.isBlank()
                    ? "Created a study task"
                    : "Created task: " + topic;
        }

        if (LearningEventTypes.TASK_COMPLETED.equals(eventType)) {
            return topic == null || topic.isBlank()
                    ? "Completed a study task"
                    : "Completed task: " + topic;
        }

        if (LearningEventTypes.REVIEW.equals(eventType)) {
            return topic == null || topic.isBlank()
                    ? "Reviewed learning material"
                    : "Reviewed: " + topic;
        }

        if (LearningEventTypes.NOTE.equals(eventType)) {
            return topic == null || topic.isBlank()
                    ? "Created a note"
                    : "Created note: " + topic;
        }

        return topic == null || topic.isBlank()
                ? "Learning activity"
                : topic;
    }

    private String buildDescription(
            String eventType,
            String targetType,
            String topic
    ) {
        if (LearningEventTypes.SUMMARY.equals(eventType)) {
            return "AI generated a structured summary from course material.";
        }

        if (LearningEventTypes.QUIZ.equals(eventType)) {
            return "A quiz was saved for later practice.";
        }

        if (LearningEventTypes.FLASHCARD.equals(eventType)) {
            return "Flashcards were saved for revision.";
        }

        if (LearningEventTypes.ASK.equals(eventType)) {
            return "A RAG answer was generated from uploaded course documents.";
        }

        if (LearningEventTypes.DOCUMENT_UPLOAD.equals(eventType)) {
            return "A new course document was uploaded and added to the course.";
        }

        if (LearningEventTypes.ASSIGNMENT_ANALYSIS.equals(eventType)) {
            return "AI extracted requirements, deliverables and checklist items from the assignment brief.";
        }

        if (LearningEventTypes.RUBRIC_ANALYSIS.equals(eventType)) {
            return "AI analyzed marking criteria and high-score strategies from the rubric.";
        }

        if (LearningEventTypes.REVISION_PACK.equals(eventType)) {
            return "AI generated a revision plan based on weak topics and learning progress.";
        }

        if (LearningEventTypes.CHECKLIST.equals(eventType)) {
            return "Study tasks were generated from assignment and rubric analysis.";
        }

        if (LearningEventTypes.TASK_CREATED.equals(eventType)) {
            return "A manual study task was added to the course plan.";
        }

        if (LearningEventTypes.TASK_COMPLETED.equals(eventType)) {
            return "A study task was marked as completed.";
        }

        if (LearningEventTypes.REVIEW.equals(eventType)) {
            return "The user reviewed or revised learning material.";
        }

        if (LearningEventTypes.NOTE.equals(eventType)) {
            return "A learning note was recorded.";
        }

        if (LearningTargetTypes.STUDY_TASK.equals(targetType)) {
            return "Study task activity.";
        }

        return "Learning activity recorded.";
    }

    private String buildIconType(
            String eventType,
            String targetType
    ) {
        if (LearningEventTypes.SUMMARY.equals(eventType)) {
            return "SUMMARY";
        }

        if (LearningEventTypes.QUIZ.equals(eventType)) {
            return "QUIZ";
        }

        if (LearningEventTypes.FLASHCARD.equals(eventType)) {
            return "FLASHCARD";
        }

        if (LearningEventTypes.ASK.equals(eventType)) {
            return "CHAT";
        }

        if (LearningEventTypes.DOCUMENT_UPLOAD.equals(eventType)) {
            return "DOCUMENT";
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

        if (LearningEventTypes.CHECKLIST.equals(eventType)) {
            return "CHECKLIST";
        }

        if (LearningEventTypes.TASK_CREATED.equals(eventType)) {
            return "TASK";
        }

        if (LearningEventTypes.TASK_COMPLETED.equals(eventType)) {
            return "TASK_DONE";
        }

        if (LearningTargetTypes.STUDY_TASK.equals(targetType)) {
            return "TASK";
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

    private void ensureCourseAccess(
            Long userId,
            Long courseId
    ) {
        int count =
                learningHistoryMapper.countCourseOwnership(
                        userId,
                        courseId
                );

        if (count == 0) {
            throw new BusinessException(
                    "COURSE_NOT_FOUND",
                    "Course not found or access denied."
            );
        }
    }

    private int normalizeLimit(
            Integer limit
    ) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }

        if (limit < 1) {
            return 1;
        }

        return Math.min(
                limit,
                MAX_LIMIT
        );
    }

    private int normalizeOffset(
            Integer offset
    ) {
        if (offset == null || offset < 0) {
            return 0;
        }

        return offset;
    }

    private long safeLong(
            Long value
    ) {
        return value == null
                ? 0L
                : value;
    }
}