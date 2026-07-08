package com.msc.springai.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Flashcard {

    private Long id;

    private Long userId;

    private Long courseId;

    private Long documentId;

    private String front;

    private String back;

    private String topic;

    private String difficulty;

    private String sourceType;

    private Long sourceChunkId;

    private LocalDateTime createdAt;
}