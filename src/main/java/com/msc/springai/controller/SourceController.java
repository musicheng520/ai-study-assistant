package com.msc.springai.controller;

import com.msc.springai.dto.rag.SourceChunkResponse;
import com.msc.springai.service.SourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/courses/{courseId}/sources")
public class SourceController {

    private final SourceService sourceService;

    @GetMapping("/chunks/{chunkId}")
    public SourceChunkResponse getChunkSource(
            @PathVariable Long courseId,
            @PathVariable Long chunkId
    ) {
        System.out.println("[SourceController] Receive get chunk source request.");
        System.out.println("[SourceController] courseId = " + courseId);
        System.out.println("[SourceController] chunkId = " + chunkId);

        Long currentUserId = getCurrentUserId();

        System.out.println("[SourceController] currentUserId = " + currentUserId);

        SourceChunkResponse response = sourceService.getChunkSource(
                currentUserId,
                courseId,
                chunkId
        );

        System.out.println("[SourceController] Get chunk source finished.");

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

        System.out.println("[SourceController] Authentication principal class = "
                + (principal == null ? null : principal.getClass().getName()));
        System.out.println("[SourceController] Authentication principal = " + principal);

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