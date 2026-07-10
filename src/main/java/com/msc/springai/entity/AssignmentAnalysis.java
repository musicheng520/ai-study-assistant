package com.msc.springai.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AssignmentAnalysis {

    private Long id;

    private Long userId;

    private Long courseId;

    private Long documentId;

    private String requirementsJson;

    private String deliverablesJson;

    private LocalDateTime deadline;

    private String checklistJson;

    private String highScoreTips;

    private String suggestedStructureJson;

    private String riskWarningsJson;

    private LocalDateTime createdAt;
}