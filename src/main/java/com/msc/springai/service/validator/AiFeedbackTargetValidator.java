package com.msc.springai.service.validator;

import com.msc.springai.exception.BusinessException;
import com.msc.springai.mapper.AiFeedbackMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
@RequiredArgsConstructor
public class AiFeedbackTargetValidator {

    private static final String TARGET_ANSWER = "ANSWER";
    private static final String TARGET_SUMMARY = "SUMMARY";
    private static final String TARGET_QUIZ = "QUIZ";
    private static final String TARGET_FLASHCARD = "FLASHCARD";

    private static final String TARGET_ASSIGNMENT_ANALYSIS =
            "ASSIGNMENT_ANALYSIS";

    private static final String TARGET_RUBRIC_ANALYSIS =
            "RUBRIC_ANALYSIS";

    private static final String TARGET_REVISION_PACK =
            "REVISION_PACK";

    private final AiFeedbackMapper aiFeedbackMapper;

    /**
     * 检查用户是否有权对指定 AI 输出提交反馈。
     *
     * 验证顺序：
     *
     * 1. 检查 userId
     * 2. 检查 courseId
     * 3. 检查 targetType
     * 4. 检查 targetId
     * 5. 检查 course 是否属于当前用户
     * 6. 根据 targetType 检查目标记录
     */
    public void validateTargetOwnership(
            Long userId,
            Long courseId,
            String targetType,
            Long targetId
    ) {
        validateBasicFields(
                userId,
                courseId,
                targetType,
                targetId
        );

        validateCourseOwnership(
                userId,
                courseId
        );

        String normalizedTargetType = targetType
                .trim()
                .toUpperCase(Locale.ROOT);

        int targetCount = countTarget(
                userId,
                courseId,
                normalizedTargetType,
                targetId
        );

        if (targetCount <= 0) {
            throw new BusinessException(
                    "FEEDBACK_TARGET_NOT_FOUND",
                    "AI output not found or access denied."
            );
        }
    }

    /**
     * 检查基础参数。
     *
     * 这里即使 Service 将来也做一次校验，
     * Validator 仍然保留自己的防御性校验。
     */
    private void validateBasicFields(
            Long userId,
            Long courseId,
            String targetType,
            Long targetId
    ) {
        if (userId == null) {
            throw new BusinessException(
                    "UNAUTHORIZED",
                    "Current user is required."
            );
        }

        if (courseId == null || courseId <= 0) {
            throw new BusinessException(
                    "INVALID_COURSE_ID",
                    "Course id is required."
            );
        }

        if (targetType == null || targetType.isBlank()) {
            throw new BusinessException(
                    "INVALID_FEEDBACK_TARGET_TYPE",
                    "Feedback target type is required."
            );
        }

        if (targetId == null || targetId <= 0) {
            throw new BusinessException(
                    "INVALID_FEEDBACK_TARGET_ID",
                    "Feedback target id is required."
            );
        }
    }

    /**
     * 检查课程归属。
     *
     * 不能因为 target 查询包含 courseId，
     * 就省略 course 自身的权限检查。
     *
     * 分开检查可以返回更明确的 COURSE_NOT_FOUND。
     */
    private void validateCourseOwnership(
            Long userId,
            Long courseId
    ) {
        int courseCount = aiFeedbackMapper.countCourseOwnership(
                userId,
                courseId
        );

        if (courseCount <= 0) {
            throw new BusinessException(
                    "COURSE_NOT_FOUND",
                    "Course not found or access denied."
            );
        }
    }

    /**
     * 根据 targetType 调用固定 SQL。
     *
     * 不使用动态表名，避免 SQL 注入。
     */
    private int countTarget(
            Long userId,
            Long courseId,
            String targetType,
            Long targetId
    ) {
        return switch (targetType) {
            case TARGET_ANSWER ->
                    aiFeedbackMapper.countAnswerTarget(
                            userId,
                            courseId,
                            targetId
                    );

            case TARGET_SUMMARY ->
                    aiFeedbackMapper.countSummaryTarget(
                            userId,
                            courseId,
                            targetId
                    );

            case TARGET_QUIZ ->
                    aiFeedbackMapper.countQuizTarget(
                            userId,
                            courseId,
                            targetId
                    );

            case TARGET_FLASHCARD ->
                    aiFeedbackMapper.countFlashcardTarget(
                            userId,
                            courseId,
                            targetId
                    );

            case TARGET_ASSIGNMENT_ANALYSIS ->
                    aiFeedbackMapper.countAssignmentAnalysisTarget(
                            userId,
                            courseId,
                            targetId
                    );

            case TARGET_RUBRIC_ANALYSIS ->
                    aiFeedbackMapper.countRubricAnalysisTarget(
                            userId,
                            courseId,
                            targetId
                    );

            case TARGET_REVISION_PACK ->
                    aiFeedbackMapper.countRevisionPackTarget(
                            userId,
                            courseId,
                            targetId
                    );

            default -> throw new BusinessException(
                    "INVALID_FEEDBACK_TARGET_TYPE",
                    "Unsupported feedback target type."
            );
        };
    }
}