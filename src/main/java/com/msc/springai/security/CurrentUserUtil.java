package com.msc.springai.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class CurrentUserUtil {

    private CurrentUserUtil() {
    }

    public static Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getPrincipal() == null) {
            throw new IllegalArgumentException("Unauthenticated user");
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof Long userId) {
            return userId;
        }

        return Long.valueOf(principal.toString());
    }
}