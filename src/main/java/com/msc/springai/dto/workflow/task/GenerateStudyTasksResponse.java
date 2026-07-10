package com.msc.springai.dto.workflow.task;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GenerateStudyTasksResponse {

    private Long courseId;

    private Integer createdCount;

    private String message;

    private List<StudyTaskResponse> tasks;
}