package com.msc.springai.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Quiz {

    private Long id;

    private Long userId;

    private Long courseId;

    private Long documentId;

    private String title;

    private String difficulty;

    private String sourceScope;

    private Integer questionCount;

    private LocalDateTime createdAt;
}