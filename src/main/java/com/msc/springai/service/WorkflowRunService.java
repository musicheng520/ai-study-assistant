package com.msc.springai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.msc.springai.entity.AiWorkflowRun;
import com.msc.springai.entity.AiWorkflowStep;
import com.msc.springai.entity.WorkflowStatus;
import com.msc.springai.entity.WorkflowType;
import com.msc.springai.exception.BusinessException;
import com.msc.springai.mapper.AiWorkflowRunMapper;
import com.msc.springai.mapper.AiWorkflowStepMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WorkflowRunService {

    private final AiWorkflowRunMapper workflowRunMapper;
    private final AiWorkflowStepMapper workflowStepMapper;
    private final ObjectMapper objectMapper;

    public AiWorkflowRun startRun(Long userId,
                                  Long courseId,
                                  WorkflowType workflowType,
                                  Object input) {
        AiWorkflowRun run = new AiWorkflowRun();
        run.setUserId(userId);
        run.setCourseId(courseId);
        run.setWorkflowType(workflowType.name());
        run.setStatus(WorkflowStatus.RUNNING.name());
        run.setInputJson(toJson(input));
        run.setOutputJson(null);
        run.setErrorMessage(null);
        run.setStartedAt(LocalDateTime.now());
        run.setCompletedAt(null);

        workflowRunMapper.insert(run);
        return run;
    }

    public void completeRun(Long workflowRunId, Object output) {
        int updated = workflowRunMapper.markSuccess(workflowRunId, toJson(output));
        if (updated == 0) {
            throw new BusinessException("Workflow run not found");
        }
    }

    public void failRun(Long workflowRunId, String errorMessage) {
        int updated = workflowRunMapper.markFailed(workflowRunId, trimError(errorMessage));
        if (updated == 0) {
            throw new BusinessException("Workflow run not found");
        }
    }

    public AiWorkflowStep startStep(Long workflowRunId, String stepName) {
        AiWorkflowRun run = workflowRunMapper.findById(workflowRunId);
        if (run == null) {
            throw new BusinessException("Workflow run not found");
        }

        AiWorkflowStep step = new AiWorkflowStep();
        step.setWorkflowRunId(workflowRunId);
        step.setStepName(stepName);
        step.setStatus(WorkflowStatus.RUNNING.name());
        step.setStartedAt(LocalDateTime.now());
        step.setCompletedAt(null);
        step.setErrorMessage(null);

        workflowStepMapper.insert(step);
        return step;
    }

    public void completeStep(Long stepId) {
        int updated = workflowStepMapper.markSuccess(stepId);
        if (updated == 0) {
            throw new BusinessException("Workflow step not found");
        }
    }

    public void failStep(Long stepId, String errorMessage) {
        int updated = workflowStepMapper.markFailed(stepId, trimError(errorMessage));
        if (updated == 0) {
            throw new BusinessException("Workflow step not found");
        }
    }

    public AiWorkflowRun getWorkflowStatus(Long workflowRunId, Long currentUserId) {
        AiWorkflowRun run = workflowRunMapper.findByIdAndUserId(workflowRunId, currentUserId);
        if (run == null) {
            throw new BusinessException("Workflow run not found or access denied");
        }
        return run;
    }

    public List<AiWorkflowStep> getWorkflowSteps(Long workflowRunId, Long currentUserId) {
        AiWorkflowRun run = workflowRunMapper.findByIdAndUserId(workflowRunId, currentUserId);
        if (run == null) {
            throw new BusinessException("Workflow run not found or access denied");
        }

        return workflowStepMapper.findByWorkflowRunId(workflowRunId);
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }

        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new BusinessException("Failed to convert workflow data to JSON");
        }
    }

    private String trimError(String errorMessage) {
        if (errorMessage == null || errorMessage.isBlank()) {
            return "Unknown workflow error";
        }

        if (errorMessage.length() > 2000) {
            return errorMessage.substring(0, 2000);
        }

        return errorMessage;
    }
}