package com.msc.springai.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Summary {

    private Long id;

    private Long userId;

    private Long courseId;

    private Long documentId;

    private String title;

    private String summary;

    private String keyConceptsJson;

    private String definitionsJson;

    private String revisionNotes;

    private String sourceScope;

    private LocalDateTime createdAt;
}