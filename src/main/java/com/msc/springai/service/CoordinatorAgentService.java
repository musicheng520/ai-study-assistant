package com.msc.springai.service;

import com.msc.springai.dto.workflow.WorkflowRunRequest;
import com.msc.springai.dto.workflow.WorkflowRunResponse;
import com.msc.springai.dto.workflow.assignment.AssignmentAnalyzeRequest;
import com.msc.springai.dto.workflow.revision.GenerateRevisionPackRequest;
import com.msc.springai.dto.workflow.rubric.RubricAnalyzeRequest;
import com.msc.springai.dto.workflow.task.GenerateStudyTasksRequest;
import com.msc.springai.entity.AiWorkflowRun;
import com.msc.springai.entity.AiWorkflowStep;
import com.msc.springai.entity.WorkflowStatus;
import com.msc.springai.entity.WorkflowType;
import com.msc.springai.exception.BusinessException;
import com.msc.springai.service.workflow.WorkflowIntentRouter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CoordinatorAgentService {

    private final WorkflowIntentRouter workflowIntentRouter;
    private final WorkflowRunService workflowRunService;

    private final AssignmentAnalysisService assignmentAnalysisService;
    private final RubricAnalysisService rubricAnalysisService;
    private final StudyTaskService studyTaskService;
    private final RevisionPackService revisionPackService;

    public WorkflowRunResponse runWorkflow(Long currentUserId,
                                           WorkflowRunRequest request) {
        validateRequest(currentUserId, request);

        WorkflowType intent = workflowIntentRouter.route(request.getMessage());

        Map<String, Object> input = new HashMap<>();
        input.put("courseId", request.getCourseId());
        input.put("documentId", request.getDocumentId());
        input.put("message", request.getMessage());

        AiWorkflowRun workflowRun = workflowRunService.startRun(
                currentUserId,
                request.getCourseId(),
                intent,
                input
        );

        Long workflowRunId = workflowRun.getId();

        AiWorkflowStep intentStep = null;
        AiWorkflowStep executionStep = null;

        try {
            intentStep = workflowRunService.startStep(
                    workflowRunId,
                    "INTENT_CLASSIFICATION"
            );

            workflowRunService.completeStep(intentStep.getId());

            executionStep = workflowRunService.startStep(
                    workflowRunId,
                    "EXECUTE_" + intent.name()
            );

            Object result = executeIntent(
                    currentUserId,
                    request,
                    intent
            );

            workflowRunService.completeStep(executionStep.getId());

            Map<String, Object> output = buildWorkflowOutput(
                    intent,
                    result
            );

            workflowRunService.completeRun(
                    workflowRunId,
                    output
            );

            return new WorkflowRunResponse(
                    workflowRunId,
                    intent.name(),
                    WorkflowStatus.SUCCESS.name(),
                    "Workflow executed successfully.",
                    result
            );

        } catch (Exception e) {
            String errorMessage = e.getMessage() == null
                    ? e.getClass().getSimpleName()
                    : e.getMessage();

            if (executionStep != null) {
                workflowRunService.failStep(
                        executionStep.getId(),
                        errorMessage
                );
            } else if (intentStep != null) {
                workflowRunService.failStep(
                        intentStep.getId(),
                        errorMessage
                );
            }

            workflowRunService.failRun(
                    workflowRunId,
                    errorMessage
            );

            throw e;
        }
    }

    private Object executeIntent(Long currentUserId,
                                 WorkflowRunRequest request,
                                 WorkflowType intent) {
        return switch (intent) {
            case ASSIGNMENT_ANALYSIS -> executeAssignmentAnalysis(
                    currentUserId,
                    request
            );

            case RUBRIC_ANALYSIS -> executeRubricAnalysis(
                    currentUserId,
                    request
            );

            case CHECKLIST -> executeChecklistGeneration(
                    currentUserId,
                    request
            );

            case REVISION_PLAN -> executeRevisionPackGeneration(
                    currentUserId,
                    request
            );

            default -> throw new BusinessException(
                    "WORKFLOW_NOT_SUPPORTED",
                    "This workflow is not connected yet: " + intent.name()
            );
        };
    }

    private Object executeAssignmentAnalysis(Long currentUserId,
                                             WorkflowRunRequest request) {
        requireDocumentId(request);

        AssignmentAnalyzeRequest analyzeRequest = new AssignmentAnalyzeRequest();
        analyzeRequest.setForce(false);

        return assignmentAnalysisService.analyzeAssignment(
                currentUserId,
                request.getDocumentId(),
                analyzeRequest
        );
    }

    private Object executeRubricAnalysis(Long currentUserId,
                                         WorkflowRunRequest request) {
        requireDocumentId(request);

        RubricAnalyzeRequest analyzeRequest = new RubricAnalyzeRequest();
        analyzeRequest.setForce(false);

        return rubricAnalysisService.analyzeRubric(
                currentUserId,
                request.getDocumentId(),
                analyzeRequest
        );
    }

    private Object executeChecklistGeneration(Long currentUserId,
                                              WorkflowRunRequest request) {
        GenerateStudyTasksRequest generateRequest = new GenerateStudyTasksRequest();
        generateRequest.setIncludeAssignment(true);
        generateRequest.setIncludeRubric(true);
        generateRequest.setSkipExisting(true);
        generateRequest.setMaxTasks(20);

        return studyTaskService.generateTasks(
                currentUserId,
                request.getCourseId(),
                generateRequest
        );
    }

    private Object executeRevisionPackGeneration(Long currentUserId,
                                                 WorkflowRunRequest request) {
        GenerateRevisionPackRequest generateRequest = new GenerateRevisionPackRequest();
        generateRequest.setMaxWeakTopics(5);
        generateRequest.setMaxRelatedChunks(3);

        return revisionPackService.generateRevisionPack(
                currentUserId,
                request.getCourseId(),
                generateRequest
        );
    }

    private void validateRequest(Long currentUserId,
                                 WorkflowRunRequest request) {
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

    private void requireDocumentId(WorkflowRunRequest request) {
        if (request.getDocumentId() == null) {
            throw new BusinessException(
                    "DOCUMENT_ID_REQUIRED",
                    "documentId is required for this workflow."
            );
        }
    }

    private Map<String, Object> buildWorkflowOutput(WorkflowType intent,
                                                    Object result) {
        Map<String, Object> output = new HashMap<>();

        output.put("intent", intent.name());
        output.put("resultType", result == null ? null : result.getClass().getSimpleName());
        output.put("message", "Workflow executed successfully.");

        if (result == null) {
            return output;
        }

        try {
            switch (intent) {
                case REVISION_PLAN -> {
                    var response = (com.msc.springai.dto.workflow.revision.RevisionPackResponse) result;
                    output.put("resultId", response.getId());
                    output.put("courseId", response.getCourseId());
                    output.put("title", response.getTitle());
                    output.put("createdAt", response.getCreatedAt() == null ? null : response.getCreatedAt().toString());
                }

                case CHECKLIST -> {
                    var response = (com.msc.springai.dto.workflow.task.GenerateStudyTasksResponse) result;
                    output.put("courseId", response.getCourseId());
                    output.put("createdCount", response.getCreatedCount());
                }

                case RUBRIC_ANALYSIS -> {
                    var response = (com.msc.springai.dto.workflow.rubric.RubricAnalysisResponse) result;
                    output.put("resultId", response.getId());
                    output.put("courseId", response.getCourseId());
                    output.put("documentId", response.getDocumentId());
                    output.put("createdAt", response.getCreatedAt() == null ? null : response.getCreatedAt().toString());
                }

                case ASSIGNMENT_ANALYSIS -> {
                    var response = (com.msc.springai.dto.workflow.assignment.AssignmentAnalysisResponse) result;
                    output.put("resultId", response.getId());
                    output.put("courseId", response.getCourseId());
                    output.put("documentId", response.getDocumentId());
                    output.put("createdAt", response.getCreatedAt() == null ? null : response.getCreatedAt().toString());
                }

                default -> output.put("summary", "Workflow result generated.");
            }

        } catch (Exception e) {
            output.put("summary", "Workflow result generated, but summary extraction failed.");
            output.put("summaryError", e.getMessage());
        }

        return output;
    }
}