package com.msc.springai.controller;

import com.msc.springai.dto.feedback.AiFeedbackCreateRequest;
import com.msc.springai.dto.feedback.AiFeedbackResponse;
import com.msc.springai.security.CurrentUserUtil;
import com.msc.springai.service.AiFeedbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class AiFeedbackController {

    private final AiFeedbackService aiFeedbackService;

    /**
     * 用户对某个 AI 输出提交反馈。
     *
     * 支持：
     *
     * HELPFUL
     * NOT_HELPFUL
     * INACCURATE
     */
    @PostMapping("/api/feedback")
    public AiFeedbackResponse createFeedback(
            @RequestBody AiFeedbackCreateRequest request
    ) {
        Long currentUserId =
                CurrentUserUtil.getCurrentUserId();

        return aiFeedbackService.createFeedback(
                currentUserId,
                request
        );
    }

    /**
     * 查询当前登录用户提交的全部反馈。
     *
     * 不接收 userId 参数，
     * 只能查询 JWT 所代表用户的数据。
     */
    @GetMapping("/api/feedback/my")
    public List<AiFeedbackResponse> getMyFeedback() {
        Long currentUserId =
                CurrentUserUtil.getCurrentUserId();

        return aiFeedbackService.getMyFeedback(
                currentUserId
        );
    }
}