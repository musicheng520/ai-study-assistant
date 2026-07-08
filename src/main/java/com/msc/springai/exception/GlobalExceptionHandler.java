package com.msc.springai.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, Object>> handleBusinessException(BusinessException ex) {
        HttpStatus status = resolveStatus(ex.getCode());

        return ResponseEntity
                .status(status)
                .body(Map.of(
                        "timestamp", LocalDateTime.now(),
                        "status", status.value(),
                        "error", status.getReasonPhrase(),
                        "code", ex.getCode(),
                        "message", ex.getMessage()
                ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(IllegalArgumentException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "timestamp", LocalDateTime.now(),
                        "status", 400,
                        "error", "Bad Request",
                        "code", "INVALID_REQUEST",
                        "message", ex.getMessage()
                ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("Validation failed");

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "timestamp", LocalDateTime.now(),
                        "status", 400,
                        "error", "Validation Failed",
                        "code", "VALIDATION_FAILED",
                        "message", message
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpectedException(Exception ex) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "timestamp", LocalDateTime.now(),
                        "status", 500,
                        "error", "Internal Server Error",
                        "code", "INTERNAL_SERVER_ERROR",
                        "message", "Unexpected server error."
                ));
    }

    private HttpStatus resolveStatus(String code) {
        if (code == null) {
            return HttpStatus.BAD_REQUEST;
        }

        return switch (code) {
            case "UNAUTHORIZED" -> HttpStatus.UNAUTHORIZED;

            case "FORBIDDEN",
                    "FORBIDDEN_DRAFT",
                    "COURSE_ACCESS_DENIED",
                    "DOCUMENT_ACCESS_DENIED" -> HttpStatus.FORBIDDEN;

            case "DRAFT_NOT_FOUND",
                    "COURSE_NOT_FOUND",
                    "DOCUMENT_NOT_FOUND",
                    "SUMMARY_NOT_FOUND",
                    "QUIZ_NOT_FOUND",
                    "FLASHCARD_NOT_FOUND" -> HttpStatus.NOT_FOUND;

            case "AI_OUTPUT_INVALID",
                    "INVALID_DRAFT_KEY",
                    "INVALID_DRAFT_SCOPE",
                    "INVALID_DRAFT_TYPE",
                    "INVALID_DRAFT_VALUE",
                    "INVALID_DRAFT_KEY_PARAMS",
                    "PARAM_HASH_FAILED",
                    "REDIS_DRAFT_SAVE_FAILED",
                    "REDIS_DRAFT_READ_FAILED",
                    "REDIS_DRAFT_DELETE_FAILED",
                    "DOCUMENT_NOT_READY",
                    "NO_READY_DOCUMENTS",
                    "VALIDATION_FAILED" -> HttpStatus.BAD_REQUEST;

            default -> HttpStatus.BAD_REQUEST;
        };
    }
}