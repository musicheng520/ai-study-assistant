package com.msc.springai.controller;

import com.msc.springai.dto.workflow.WorkflowRunRequest;
import com.msc.springai.dto.workflow.WorkflowRunResponse;
import com.msc.springai.entity.AiWorkflowRun;
import com.msc.springai.entity.AiWorkflowStep;
import com.msc.springai.security.CurrentUserUtil;
import com.msc.springai.service.CoordinatorAgentService;
import com.msc.springai.service.WorkflowRunService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/workflows")
@RequiredArgsConstructor
public class WorkflowController {

    private final CoordinatorAgentService coordinatorAgentService;
    private final WorkflowRunService workflowRunService;

    @PostMapping("/run")
    public WorkflowRunResponse runWorkflow(@RequestBody WorkflowRunRequest request) {
        Long currentUserId = CurrentUserUtil.getCurrentUserId();

        return coordinatorAgentService.runWorkflow(
                currentUserId,
                request
        );
    }

    @GetMapping("/{workflowRunId}/status")
    public AiWorkflowRun getWorkflowStatus(@PathVariable Long workflowRunId) {
        Long currentUserId = CurrentUserUtil.getCurrentUserId();

        return workflowRunService.getWorkflowStatus(
                workflowRunId,
                currentUserId
        );
    }

    @GetMapping("/{workflowRunId}/steps")
    public List<AiWorkflowStep> getWorkflowSteps(@PathVariable Long workflowRunId) {
        Long currentUserId = CurrentUserUtil.getCurrentUserId();

        return workflowRunService.getWorkflowSteps(
                workflowRunId,
                currentUserId
        );
    }
}