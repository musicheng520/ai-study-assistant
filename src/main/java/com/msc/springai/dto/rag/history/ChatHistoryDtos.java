package com.msc.springai.dto.rag.history;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

public final class ChatHistoryDtos {

    private ChatHistoryDtos() {
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatSessionListItemResponse {

        private Long id;

        private Long courseId;

        private String title;

        private String scopeType;

        private Long documentId;

        private Long messageCount;

        private String lastMessagePreview;

        private LocalDateTime createdAt;

        private LocalDateTime updatedAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatSessionDetailResponse {

        private Long id;

        private Long courseId;

        private String title;

        private String scopeType;

        private Long documentId;

        private LocalDateTime createdAt;

        private LocalDateTime updatedAt;

        private List<ChatMessageHistoryResponse> messages;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatMessageHistoryResponse {

        private Long id;

        private String role;

        private String content;

        private String workflowType;

        private Boolean noAnswer;

        private String modelName;

        private LocalDateTime createdAt;

        private List<ChatCitationHistoryResponse> citations;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatCitationHistoryResponse {

        private Integer citationIndex;

        private Long documentId;

        private Long chunkId;

        private String fileName;

        private Integer pageNumber;

        private String sectionTitle;

        private String snippet;

        private LocalDateTime createdAt;
    }

    /*
     * Mapper row.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatSessionDetailRow {

        private Long id;

        private Long courseId;

        private String title;

        private String scopeType;

        private Long documentId;

        private LocalDateTime createdAt;

        private LocalDateTime updatedAt;
    }

    /*
     * Mapper row.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatMessageHistoryRow {

        private Long id;

        private String role;

        private String content;

        private String workflowType;

        private Boolean noAnswer;

        private String modelName;

        private LocalDateTime createdAt;
    }

    /*
     * Mapper row.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatCitationHistoryRow {

        private Long messageId;

        private Long documentId;

        private Long chunkId;

        private String fileName;

        private Integer pageNumber;

        private String sectionTitle;

        private String snippet;

        private LocalDateTime createdAt;
    }
}