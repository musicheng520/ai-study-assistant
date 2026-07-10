package com.msc.springai.service;

import com.msc.springai.dto.workflow.tool.StudyToolDtos.CourseDocumentToolResult;
import com.msc.springai.dto.workflow.tool.StudyToolDtos.CourseProgressToolResult;
import com.msc.springai.dto.workflow.tool.StudyToolDtos.LearningHistoryToolResult;
import com.msc.springai.dto.workflow.tool.StudyToolDtos.NoteToolResult;
import com.msc.springai.dto.workflow.tool.StudyToolDtos.WrongAnswersToolResult;
import com.msc.springai.dto.workflow.tool.StudyToolDtos.WrongAnswerItem;
import com.msc.springai.dto.workflow.tool.StudyToolDtos.WrongAnswerTopicSummary;
import com.msc.springai.exception.BusinessException;
import com.msc.springai.mapper.StudyAgentToolMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StudyAgentToolService {

    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 50;

    private final StudyAgentToolMapper studyAgentToolMapper;

    public List<CourseDocumentToolResult> getCourseDocumentsTool(Long userId,
                                                                 Long courseId,
                                                                 String documentType) {
        ensureCourseAccess(userId, courseId);

        String normalizedDocumentType = normalizeBlankToNull(documentType);

        return studyAgentToolMapper.findReadyDocuments(
                userId,
                courseId,
                normalizedDocumentType
        );
    }

    public List<LearningHistoryToolResult> searchLearningHistoryTool(Long userId,
                                                                     Long courseId,
                                                                     Integer limit) {
        ensureCourseAccess(userId, courseId);

        return studyAgentToolMapper.findRecentLearningHistory(
                userId,
                courseId,
                normalizeLimit(limit)
        );
    }

    public WrongAnswersToolResult getWrongAnswersTool(Long userId,
                                                      Long courseId,
                                                      Boolean resolved,
                                                      Integer limit) {
        ensureCourseAccess(userId, courseId);

        int safeLimit = normalizeLimit(limit);

        List<WrongAnswerTopicSummary> weakTopics =
                studyAgentToolMapper.findWeakTopicSummaries(
                        userId,
                        courseId,
                        resolved,
                        safeLimit
                );

        List<WrongAnswerItem> wrongAnswerItems =
                studyAgentToolMapper.findWrongAnswerItems(
                        userId,
                        courseId,
                        resolved,
                        safeLimit
                );

        return new WrongAnswersToolResult(weakTopics, wrongAnswerItems);
    }

    public List<NoteToolResult> searchNotesTool(Long userId,
                                                Long courseId,
                                                String keyword,
                                                String topic,
                                                Integer limit) {
        ensureCourseAccess(userId, courseId);

        return studyAgentToolMapper.searchNotes(
                userId,
                courseId,
                normalizeBlankToNull(keyword),
                normalizeBlankToNull(topic),
                normalizeLimit(limit)
        );
    }

    public CourseProgressToolResult getCourseProgressTool(Long userId,
                                                          Long courseId) {
        ensureCourseAccess(userId, courseId);

        BigDecimal progressScore =
                nullToZero(studyAgentToolMapper.findCourseProgressScore(userId, courseId));

        BigDecimal averageQuizScore =
                nullToZero(studyAgentToolMapper.findAverageQuizScore(userId, courseId));

        int wrongAnswerCount =
                studyAgentToolMapper.countWrongAnswers(userId, courseId);

        int unresolvedWrongAnswerCount =
                studyAgentToolMapper.countUnresolvedWrongAnswers(userId, courseId);

        List<WrongAnswerTopicSummary> weakTopics =
                studyAgentToolMapper.findWeakTopicSummaries(
                        userId,
                        courseId,
                        false,
                        5
                );

        List<LearningHistoryToolResult> recentActivity =
                studyAgentToolMapper.findRecentLearningHistory(
                        userId,
                        courseId,
                        10
                );

        String recommendedNextReview = buildRecommendedNextReview(weakTopics);

        return new CourseProgressToolResult(
                progressScore,
                averageQuizScore,
                wrongAnswerCount,
                unresolvedWrongAnswerCount,
                weakTopics,
                recommendedNextReview,
                recentActivity
        );
    }

    private void ensureCourseAccess(Long userId, Long courseId) {
        if (userId == null) {
            throw new BusinessException("Current user is required");
        }

        if (courseId == null) {
            throw new BusinessException("Course id is required");
        }

        int count = studyAgentToolMapper.countCourseOwnership(userId, courseId);

        if (count == 0) {
            throw new BusinessException("Course not found or access denied");
        }
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_LIMIT;
        }

        return Math.min(limit, MAX_LIMIT);
    }

    private String normalizeBlankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    private BigDecimal nullToZero(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }

        return value;
    }

    private String buildRecommendedNextReview(List<WrongAnswerTopicSummary> weakTopics) {
        if (weakTopics == null || weakTopics.isEmpty()) {
            return "No weak topic found yet. Try completing a quiz first.";
        }

        WrongAnswerTopicSummary firstWeakTopic = weakTopics.get(0);

        return "Review " + firstWeakTopic.getTopic()
                + " first because it has "
                + firstWeakTopic.getUnresolvedCount()
                + " unresolved wrong answers.";
    }
}