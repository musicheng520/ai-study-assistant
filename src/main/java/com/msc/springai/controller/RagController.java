package com.msc.springai.controller;

import com.msc.springai.dto.rag.RagAskRequest;
import com.msc.springai.dto.rag.RagAskResponse;
import com.msc.springai.service.RagService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/courses/{courseId}/chat")
public class RagController {

    private final RagService ragService;

    @PostMapping("/ask")
    public RagAskResponse askCourse(
            @PathVariable Long courseId,
            @RequestBody RagAskRequest request
    ) {
        System.out.println("[RagController] Receive course RAG ask request.");
        System.out.println("[RagController] courseId = " + courseId);

        Long currentUserId = getCurrentUserId();

        System.out.println("[RagController] currentUserId = " + currentUserId);
        System.out.println("[RagController] question = "
                + (request == null ? null : request.getQuestion()));

        RagAskResponse response = ragService.askCourse(
                currentUserId,
                courseId,
                request
        );

        System.out.println("[RagController] Course RAG ask finished.");
        System.out.println("[RagController] sessionId = " + response.getSessionId());
        System.out.println("[RagController] assistantMessageId = " + response.getAssistantMessageId());
        System.out.println("[RagController] noAnswer = " + response.getNoAnswer());

        return response;
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("User is not authenticated");
        }

        Object principal = authentication.getPrincipal();

        System.out.println("[RagController] Authentication principal class = "
                + (principal == null ? null : principal.getClass().getName()));
        System.out.println("[RagController] Authentication principal = " + principal);

        if (principal instanceof Long userId) {
            return userId;
        }

        if (principal instanceof Integer userId) {
            return userId.longValue();
        }

        if (principal instanceof String value) {
            if ("anonymousUser".equals(value)) {
                throw new RuntimeException("User is not authenticated");
            }

            try {
                return Long.parseLong(value);
            } catch (Exception e) {
                throw new RuntimeException("Invalid user id in authentication principal: " + value);
            }
        }

        Object details = authentication.getDetails();

        if (details instanceof Long userId) {
            return userId;
        }

        if (details instanceof Integer userId) {
            return userId.longValue();
        }

        throw new RuntimeException("Cannot resolve current user id from authentication");
    }
}