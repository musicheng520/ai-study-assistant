package com.msc.springai.dto.workflow;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WorkflowRunResponse {

    private Long workflowRunId;

    private String intent;

    private String status;

    private String message;

    private Object result;
}