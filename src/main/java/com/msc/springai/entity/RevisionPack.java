package com.msc.springai.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RevisionPack {

    private Long id;

    private Long userId;

    private Long courseId;

    private String title;

    private String summary;

    private String weakTopicsJson;

    private String reviewOrderJson;

    private String recommendedActionsJson;

    private String relatedDocumentsJson;

    private String studyTasksJson;

    private String suggestedFlashcardsJson;

    private Long generatedQuizId;

    private LocalDateTime createdAt;
}