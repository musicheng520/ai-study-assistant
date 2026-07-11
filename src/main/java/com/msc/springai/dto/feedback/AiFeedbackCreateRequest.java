package com.msc.springai.dto.feedback;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AiFeedbackCreateRequest {

    /**
     * Feedback 所属课程。
     *
     * 后端仍然需要检查：
     * courseId 是否属于当前登录用户。
     */
    private Long courseId;

    /**
     * 支持：
     *
     * ANSWER
     * SUMMARY
     * QUIZ
     * FLASHCARD
     * ASSIGNMENT_ANALYSIS
     * RUBRIC_ANALYSIS
     * REVISION_PACK
     */
    private String targetType;

    /**
     * 对应目标数据表中的主键 ID。
     */
    private Long targetId;

    /**
     * 支持：
     *
     * HELPFUL
     * NOT_HELPFUL
     * INACCURATE
     */
    private String rating;

    /**
     * 用户可选填写的补充说明。
     */
    private String comment;
}