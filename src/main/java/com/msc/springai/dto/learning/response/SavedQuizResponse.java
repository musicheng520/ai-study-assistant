package com.msc.springai.dto.learning.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SavedQuizResponse {

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