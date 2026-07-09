package com.msc.springai.service;

import com.msc.springai.dto.learning.response.CourseWrongAnswersResponse;
import com.msc.springai.dto.learning.response.WrongAnswerItemResponse;
import com.msc.springai.dto.learning.response.WrongAnswerResolvedResponse;
import com.msc.springai.dto.learning.response.WrongAnswerTopicGroupResponse;
import com.msc.springai.entity.Course;
import com.msc.springai.entity.WrongAnswer;
import com.msc.springai.exception.BusinessException;
import com.msc.springai.mapper.CourseMapper;
import com.msc.springai.mapper.LearningHistoryMapper;
import com.msc.springai.mapper.WrongAnswerMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WrongAnswerService {

    private final WrongAnswerMapper wrongAnswerMapper;
    private final CourseMapper courseMapper;
    private final LearningHistoryMapper learningHistoryMapper;
    private final StudyStreakService studyStreakService;

    public CourseWrongAnswersResponse getCourseWrongAnswers(
            Long userId,
            Long courseId,
            Boolean resolved,
            String topic
    ) {
        validateCourseAccess(userId, courseId);

        String normalizedTopic = normalizeTopicFilter(topic);

        List<WrongAnswer> wrongAnswers = wrongAnswerMapper.findByCourse(
                userId,
                courseId,
                resolved,
                normalizedTopic
        );

        List<WrongAnswerItemResponse> items = wrongAnswers.stream()
                .map(this::toItemResponse)
                .toList();

        List<WrongAnswerTopicGroupResponse> topicGroups = buildTopicGroups(wrongAnswers);

        return new CourseWrongAnswersResponse(
                courseId,
                resolved,
                normalizedTopic,
                items.size(),
                topicGroups,
                items
        );
    }

    @Transactional
    public WrongAnswerResolvedResponse markResolved(
            Long userId,
            Long wrongAnswerId
    ) {
        WrongAnswer wrongAnswer = wrongAnswerMapper.findByIdAndUserId(
                wrongAnswerId,
                userId
        );

        if (wrongAnswer == null) {
            throw new BusinessException(
                    "WRONG_ANSWER_NOT_FOUND",
                    "Wrong answer not found."
            );
        }

        int updated = wrongAnswerMapper.markResolved(
                wrongAnswerId,
                userId
        );

        if (updated == 0) {
            throw new BusinessException(
                    "WRONG_ANSWER_UPDATE_FAILED",
                    "Failed to mark wrong answer as resolved."
            );
        }

        learningHistoryMapper.insertLearningHistory(
                userId,
                wrongAnswer.getCourseId(),
                "REVIEW",
                "TOPIC",
                wrongAnswerId,
                wrongAnswer.getTopic()
        );

        studyStreakService.updateStreak(userId);

        return new WrongAnswerResolvedResponse(
                wrongAnswerId,
                true,
                wrongAnswer.getTopic()
        );
    }

    @Transactional
    public void deleteWrongAnswer(
            Long userId,
            Long wrongAnswerId
    ) {
        WrongAnswer wrongAnswer = wrongAnswerMapper.findByIdAndUserId(
                wrongAnswerId,
                userId
        );

        if (wrongAnswer == null) {
            throw new BusinessException(
                    "WRONG_ANSWER_NOT_FOUND",
                    "Wrong answer not found."
            );
        }

        int deleted = wrongAnswerMapper.deleteByIdAndUserId(
                wrongAnswerId,
                userId
        );

        if (deleted == 0) {
            throw new BusinessException(
                    "WRONG_ANSWER_DELETE_FAILED",
                    "Failed to delete wrong answer."
            );
        }
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

    private String normalizeTopicFilter(String topic) {
        if (topic == null || topic.isBlank()) {
            return null;
        }

        return topic.trim();
    }

    private WrongAnswerItemResponse toItemResponse(WrongAnswer wrongAnswer) {
        return new WrongAnswerItemResponse(
                wrongAnswer.getId(),
                wrongAnswer.getQuizId(),
                wrongAnswer.getQuestionId(),
                wrongAnswer.getTopic(),
                wrongAnswer.getUserAnswer(),
                wrongAnswer.getCorrectAnswer(),
                wrongAnswer.getExplanation(),
                wrongAnswer.getResolved(),
                wrongAnswer.getCreatedAt()
        );
    }

    private List<WrongAnswerTopicGroupResponse> buildTopicGroups(
            List<WrongAnswer> wrongAnswers
    ) {
        Map<String, List<WrongAnswer>> grouped = wrongAnswers.stream()
                .collect(Collectors.groupingBy(WrongAnswer::getTopic));

        return grouped.entrySet()
                .stream()
                .map(entry -> {
                    String topic = entry.getKey();
                    List<WrongAnswer> items = entry.getValue();

                    int count = items.size();

                    int resolvedCount = (int) items.stream()
                            .filter(item -> Boolean.TRUE.equals(item.getResolved()))
                            .count();

                    int unresolvedCount = count - resolvedCount;

                    return new WrongAnswerTopicGroupResponse(
                            topic,
                            count,
                            unresolvedCount,
                            resolvedCount
                    );
                })
                .sorted(
                        Comparator
                                .comparing(WrongAnswerTopicGroupResponse::getUnresolvedCount)
                                .reversed()
                                .thenComparing(
                                        WrongAnswerTopicGroupResponse::getCount,
                                        Comparator.reverseOrder()
                                )
                                .thenComparing(WrongAnswerTopicGroupResponse::getTopic)
                )
                .toList();
    }
}