package com.msc.springai.service;

import com.msc.springai.dto.workflow.WorkflowRunRequest;
import com.msc.springai.dto.workflow.WorkflowRunResponse;
import com.msc.springai.entity.AiWorkflowRun;
import com.msc.springai.entity.AiWorkflowStep;
import com.msc.springai.entity.WorkflowType;
import com.msc.springai.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CoordinatorAgentService {

    private final WorkflowIntentRouter workflowIntentRouter;
    private final WorkflowRunService workflowRunService;

    public WorkflowRunResponse runWorkflow(Long currentUserId,
                                           WorkflowRunRequest request) {
        validateRequest(currentUserId, request);

        WorkflowType intent = workflowIntentRouter.route(request.getMessage());

        Map<String, Object> input = new HashMap<>();
        input.put("courseId", request.getCourseId());
        input.put("documentId", request.getDocumentId());
        input.put("message", request.getMessage());

        AiWorkflowRun run = workflowRunService.startRun(
                currentUserId,
                request.getCourseId(),
                intent,
                input
        );

        AiWorkflowStep intentStep = null;

        try {
            intentStep = workflowRunService.startStep(
                    run.getId(),
                    "INTENT_CLASSIFICATION"
            );

            workflowRunService.completeStep(intentStep.getId());

            Map<String, Object> output = new HashMap<>();
            output.put("intent", intent.name());
            output.put(
                    "message",
                    "Intent routed successfully. Workflow execution will be implemented in the next modules."
            );

            workflowRunService.completeRun(run.getId(), output);

            return new WorkflowRunResponse(
                    run.getId(),
                    intent.name(),
                    "SUCCESS",
                    "Intent routed successfully. Workflow execution will be implemented in the next modules.",
                    null
            );

        } catch (Exception e) {
            if (intentStep != null) {
                workflowRunService.failStep(intentStep.getId(), e.getMessage());
            }

            workflowRunService.failRun(run.getId(), e.getMessage());

            throw e;
        }
    }

    private void validateRequest(Long currentUserId, WorkflowRunRequest request) {
        if (currentUserId == null) {
            throw new BusinessException(
                    "UNAUTHORIZED",
                    "Current user is required."
            );
        }

        if (request == null) {
            throw new BusinessException(
                    "INVALID_WORKFLOW_REQUEST",
                    "Workflow request is required."
            );
        }

        if (request.getCourseId() == null) {
            throw new BusinessException(
                    "INVALID_COURSE_ID",
                    "Course id is required."
            );
        }

        if (request.getMessage() == null || request.getMessage().isBlank()) {
            throw new BusinessException(
                    "INVALID_WORKFLOW_MESSAGE",
                    "Workflow message is required."
            );
        }
    }
}