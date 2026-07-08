package com.msc.springai.security;

import com.msc.springai.exception.BusinessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class CurrentUserUtil {

    private CurrentUserUtil() {
    }

    public static Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();

        if (authentication == null || authentication.getPrincipal() == null) {
            throw new BusinessException(
                    "UNAUTHORIZED",
                    "Unauthenticated user."
            );
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof Long userId) {
            return userId;
        }

        if (principal instanceof Integer userId) {
            return userId.longValue();
        }

        try {
            return Long.valueOf(principal.toString());

        } catch (NumberFormatException e) {
            throw new BusinessException(
                    "UNAUTHORIZED",
                    "Cannot resolve current user id."
            );
        }
    }
}