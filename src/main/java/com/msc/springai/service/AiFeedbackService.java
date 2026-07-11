package com.msc.springai.service;

import com.msc.springai.dto.feedback.AiFeedbackCreateRequest;
import com.msc.springai.dto.feedback.AiFeedbackResponse;
import com.msc.springai.entity.AiFeedback;
import com.msc.springai.exception.BusinessException;
import com.msc.springai.mapper.AiFeedbackMapper;
import com.msc.springai.service.validator.AiFeedbackTargetValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AiFeedbackService {

    /*
     * 当前允许提交反馈的 AI 输出类型。
     */
    private static final Set<String> VALID_TARGET_TYPES = Set.of(
            "ANSWER",
            "SUMMARY",
            "QUIZ",
            "FLASHCARD",
            "ASSIGNMENT_ANALYSIS",
            "RUBRIC_ANALYSIS",
            "REVISION_PACK"
    );

    /*
     * 当前支持的反馈评价类型。
     */
    private static final Set<String> VALID_RATINGS = Set.of(
            "HELPFUL",
            "NOT_HELPFUL",
            "INACCURATE"
    );

    /*
     * 虽然数据库字段是 TEXT，
     * 但产品层仍然限制用户评论长度。
     */
    private static final int MAX_COMMENT_LENGTH = 1000;

    private final AiFeedbackMapper aiFeedbackMapper;

    private final AiFeedbackTargetValidator aiFeedbackTargetValidator;

    /**
     * 创建一条 AI Feedback。
     */
    @Transactional
    public AiFeedbackResponse createFeedback(
            Long currentUserId,
            AiFeedbackCreateRequest request
    ) {
        validateCurrentUser(currentUserId);
        validateRequest(request);

        String normalizedTargetType =
                normalizeTargetType(request.getTargetType());

        String normalizedRating =
                normalizeRating(request.getRating());

        String normalizedComment =
                normalizeComment(request.getComment());

        /*
         * 验证：
         *
         * 1. course 属于当前用户
         * 2. targetId 存在
         * 3. targetId 属于当前用户
         * 4. targetId 属于指定 course
         * 5. ANSWER 必须是 ASSISTANT 消息
         */
        aiFeedbackTargetValidator.validateTargetOwnership(
                currentUserId,
                request.getCourseId(),
                normalizedTargetType,
                request.getTargetId()
        );

        AiFeedback feedback = new AiFeedback();

        feedback.setUserId(currentUserId);
        feedback.setCourseId(request.getCourseId());
        feedback.setTargetType(normalizedTargetType);
        feedback.setTargetId(request.getTargetId());
        feedback.setRating(normalizedRating);
        feedback.setComment(normalizedComment);
        feedback.setCreatedAt(LocalDateTime.now());

        aiFeedbackMapper.insert(feedback);

        return toResponse(feedback);
    }

    /**
     * 查询当前登录用户提交的反馈。
     */
    @Transactional(readOnly = true)
    public List<AiFeedbackResponse> getMyFeedback(
            Long currentUserId
    ) {
        validateCurrentUser(currentUserId);

        return aiFeedbackMapper
                .findByUserId(currentUserId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * 当前用户必须来自 JWT。
     */
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

    /**
     * 检查请求体。
     *
     * courseId 和 targetId 的进一步检查，
     * 会交给 AiFeedbackTargetValidator。
     */
    private void validateRequest(
            AiFeedbackCreateRequest request
    ) {
        if (request == null) {
            throw new BusinessException(
                    "INVALID_FEEDBACK_REQUEST",
                    "Feedback request is required."
            );
        }

        if (request.getCourseId() == null
                || request.getCourseId() <= 0) {
            throw new BusinessException(
                    "INVALID_COURSE_ID",
                    "Course id is required."
            );
        }

        if (request.getTargetId() == null
                || request.getTargetId() <= 0) {
            throw new BusinessException(
                    "INVALID_FEEDBACK_TARGET_ID",
                    "Feedback target id is required."
            );
        }

        if (request.getTargetType() == null
                || request.getTargetType().isBlank()) {
            throw new BusinessException(
                    "INVALID_FEEDBACK_TARGET_TYPE",
                    "Feedback target type is required."
            );
        }

        if (request.getRating() == null
                || request.getRating().isBlank()) {
            throw new BusinessException(
                    "INVALID_FEEDBACK_RATING",
                    "Feedback rating is required."
            );
        }
    }

    /**
     * targetType 统一转换为大写。
     *
     * 例如：
     *
     * summary
     * Summary
     * SUMMARY
     *
     * 最终都转换为 SUMMARY。
     */
    private String normalizeTargetType(
            String targetType
    ) {
        String normalized = targetType
                .trim()
                .toUpperCase(Locale.ROOT);

        if (!VALID_TARGET_TYPES.contains(normalized)) {
            throw new BusinessException(
                    "INVALID_FEEDBACK_TARGET_TYPE",
                    "Unsupported feedback target type."
            );
        }

        return normalized;
    }

    /**
     * rating 统一转换为大写并校验。
     */
    private String normalizeRating(
            String rating
    ) {
        String normalized = rating
                .trim()
                .toUpperCase(Locale.ROOT);

        if (!VALID_RATINGS.contains(normalized)) {
            throw new BusinessException(
                    "INVALID_FEEDBACK_RATING",
                    "Unsupported feedback rating."
            );
        }

        return normalized;
    }

    /**
     * comment 可以为空。
     *
     * 空字符串统一保存为 null，
     * 避免数据库中同时出现 null、"" 和全空格三种状态。
     */
    private String normalizeComment(
            String comment
    ) {
        if (comment == null) {
            return null;
        }

        String normalized = comment.trim();

        if (normalized.isBlank()) {
            return null;
        }

        if (normalized.length() > MAX_COMMENT_LENGTH) {
            throw new BusinessException(
                    "FEEDBACK_COMMENT_TOO_LONG",
                    "Feedback comment must not exceed "
                            + MAX_COMMENT_LENGTH
                            + " characters."
            );
        }

        return normalized;
    }

    /**
     * Entity 转换为 API Response。
     */
    private AiFeedbackResponse toResponse(
            AiFeedback feedback
    ) {
        return new AiFeedbackResponse(
                feedback.getId(),
                feedback.getCourseId(),
                feedback.getTargetType(),
                feedback.getTargetId(),
                feedback.getRating(),
                feedback.getComment(),
                feedback.getCreatedAt()
        );
    }
}