package com.msc.springai.controller;

import com.msc.springai.dto.rag.history.ChatHistoryDtos.ChatSessionDetailResponse;
import com.msc.springai.dto.rag.history.ChatHistoryDtos.ChatSessionListItemResponse;
import com.msc.springai.security.CurrentUserUtil;
import com.msc.springai.service.ChatHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ChatHistoryController {

    private final ChatHistoryService chatHistoryService;

    @GetMapping("/api/courses/{courseId}/chat/sessions")
    public List<ChatSessionListItemResponse> getCourseChatSessions(
            @PathVariable Long courseId,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) Integer offset
    ) {
        Long currentUserId =
                CurrentUserUtil.getCurrentUserId();

        return chatHistoryService.getCourseChatSessions(
                currentUserId,
                courseId,
                limit,
                offset
        );
    }

    @GetMapping("/api/chat/sessions/{sessionId}")
    public ChatSessionDetailResponse getChatSessionDetail(
            @PathVariable Long sessionId
    ) {
        Long currentUserId =
                CurrentUserUtil.getCurrentUserId();

        return chatHistoryService.getChatSessionDetail(
                currentUserId,
                sessionId
        );
    }
}