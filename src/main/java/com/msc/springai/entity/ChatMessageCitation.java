package com.msc.springai.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatMessageCitation {

    private Long id;

    private Long messageId;

    private Long documentId;

    private Long chunkId;

    private String fileName;

    private Integer pageNumber;

    private String sectionTitle;

    private String snippet;

    private LocalDateTime createdAt;
}