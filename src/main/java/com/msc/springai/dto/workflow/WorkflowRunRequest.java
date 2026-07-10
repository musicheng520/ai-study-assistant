package com.msc.springai.dto.workflow;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WorkflowRunRequest {

    private Long courseId;

    private Long documentId;

    private String message;
}