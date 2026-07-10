package com.msc.springai.dto.workflow.task;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GenerateStudyTasksRequest {

    private Boolean includeAssignment;

    private Boolean includeRubric;

    private Boolean skipExisting;

    private LocalDateTime dueDate;

    private Integer maxTasks;
}