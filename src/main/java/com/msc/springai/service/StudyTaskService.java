package com.msc.springai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.msc.springai.dto.workflow.rubric.RubricCriterionResult;
import com.msc.springai.dto.workflow.task.*;
import com.msc.springai.entity.*;
import com.msc.springai.exception.BusinessException;
import com.msc.springai.mapper.AssignmentAnalysisMapper;
import com.msc.springai.mapper.RubricAnalysisMapper;
import com.msc.springai.mapper.StudyTaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class StudyTaskService {

    private final StudyTaskMapper studyTaskMapper;
    private final AssignmentAnalysisMapper assignmentAnalysisMapper;
    private final RubricAnalysisMapper rubricAnalysisMapper;
    private final ObjectMapper objectMapper;

    public StudyTaskResponse createManualTask(Long currentUserId,
                                              Long courseId,
                                              CreateStudyTaskRequest request) {
        System.out.println("[StudyTaskService] ===== createManualTask START =====");
        System.out.println("[StudyTaskService] currentUserId = " + currentUserId);
        System.out.println("[StudyTaskService] courseId = " + courseId);
        System.out.println("[StudyTaskService] request = " + request);

        try {
            System.out.println("[StudyTaskService] Step 1: validate current user");
            validateCurrentUser(currentUserId);

            System.out.println("[StudyTaskService] Step 2: validate course id");
            validateCourseId(courseId);

            System.out.println("[StudyTaskService] Step 3: validate create request");
            validateCreateRequest(request);

            System.out.println("[StudyTaskService] Step 4: ensure course access");
            ensureCourseAccess(currentUserId, courseId);
            System.out.println("[StudyTaskService] Course access OK");

            if (request.getDocumentId() != null) {
                System.out.println("[StudyTaskService] Step 5: ensure document access");
                System.out.println("[StudyTaskService] documentId = " + request.getDocumentId());

                ensureDocumentAccess(currentUserId, courseId, request.getDocumentId());
                System.out.println("[StudyTaskService] Document access OK");
            } else {
                System.out.println("[StudyTaskService] Step 5: documentId is null, skip document access check");
            }

            LocalDateTime now = LocalDateTime.now();
            System.out.println("[StudyTaskService] Step 6: build task entity");
            System.out.println("[StudyTaskService] now = " + now);

            StudyTask task = new StudyTask();
            task.setUserId(currentUserId);
            task.setCourseId(courseId);
            task.setDocumentId(request.getDocumentId());
            task.setTitle(request.getTitle().trim());
            task.setDescription(normalizeBlankToNull(request.getDescription()));
            task.setStatus(StudyTaskStatus.TODO.name());
            task.setDueDate(request.getDueDate());
            task.setSourceType(StudyTaskSourceType.MANUAL.name());
            task.setCreatedAt(now);
            task.setUpdatedAt(now);

            System.out.println("[StudyTaskService] task before insert = " + task);

            System.out.println("[StudyTaskService] Step 7: insert study_tasks");
            studyTaskMapper.insert(task);
            System.out.println("[StudyTaskService] Task inserted successfully, taskId = " + task.getId());

            System.out.println("[StudyTaskService] Step 8: insert learning_history");

            studyTaskMapper.insertLearningHistory(
                    currentUserId,
                    courseId,
                    "NOTE",
                    "COURSE",
                    courseId,
                    null,
                    now
            );

            System.out.println("[StudyTaskService] Learning history inserted successfully");

            StudyTaskResponse response = toResponse(task);

            System.out.println("[StudyTaskService] Step 9: response = " + response);
            System.out.println("[StudyTaskService] ===== createManualTask SUCCESS =====");

            return response;

        } catch (Exception e) {
            System.out.println("[StudyTaskService] ===== createManualTask FAILED =====");
            System.out.println("[StudyTaskService] Error class = " + e.getClass().getName());
            System.out.println("[StudyTaskService] Error message = " + e.getMessage());
            e.printStackTrace();

            throw e;
        }
    }

    @Transactional
    public GenerateStudyTasksResponse generateTasks(Long currentUserId,
                                                    Long courseId,
                                                    GenerateStudyTasksRequest request) {
        validateCurrentUser(currentUserId);
        validateCourseId(courseId);
        ensureCourseAccess(currentUserId, courseId);

        boolean includeAssignment = request == null
                || request.getIncludeAssignment() == null
                || Boolean.TRUE.equals(request.getIncludeAssignment());

        boolean includeRubric = request == null
                || request.getIncludeRubric() == null
                || Boolean.TRUE.equals(request.getIncludeRubric());

        boolean skipExisting = request == null
                || request.getSkipExisting() == null
                || Boolean.TRUE.equals(request.getSkipExisting());

        int maxTasks = normalizeMaxTasks(request == null ? null : request.getMaxTasks());

        LocalDateTime fallbackDueDate = request == null ? null : request.getDueDate();

        List<TaskCandidate> candidates = new ArrayList<>();

        if (includeAssignment) {
            AssignmentAnalysis assignmentAnalysis =
                    assignmentAnalysisMapper.findLatestByCourseIdAndUserId(
                            currentUserId,
                            courseId
                    );

            if (assignmentAnalysis != null) {
                candidates.addAll(buildAssignmentTaskCandidates(assignmentAnalysis, fallbackDueDate));
            }
        }

        if (includeRubric) {
            RubricAnalysis rubricAnalysis =
                    rubricAnalysisMapper.findLatestByCourseIdAndUserId(
                            currentUserId,
                            courseId
                    );

            if (rubricAnalysis != null) {
                candidates.addAll(buildRubricTaskCandidates(rubricAnalysis, fallbackDueDate));
            }
        }

        candidates = deduplicateAndLimit(candidates, maxTasks);

        if (candidates.isEmpty()) {
            throw new BusinessException(
                    "NO_CHECKLIST_SOURCE",
                    "No assignment or rubric analysis found for checklist generation."
            );
        }

        List<StudyTaskResponse> createdTasks = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (TaskCandidate candidate : candidates) {
            if (candidate.title() == null || candidate.title().isBlank()) {
                continue;
            }

            String normalizedTitle = candidate.title().trim();

            if (skipExisting) {
                int existingCount = studyTaskMapper.countByTitle(
                        currentUserId,
                        courseId,
                        normalizedTitle
                );

                if (existingCount > 0) {
                    continue;
                }
            }

            StudyTask task = new StudyTask();
            task.setUserId(currentUserId);
            task.setCourseId(courseId);
            task.setDocumentId(candidate.documentId());
            task.setTitle(normalizedTitle);
            task.setDescription(normalizeBlankToNull(candidate.description()));
            task.setStatus(StudyTaskStatus.TODO.name());
            task.setDueDate(candidate.dueDate());
            task.setSourceType(candidate.sourceType());
            task.setCreatedAt(now);
            task.setUpdatedAt(now);

            studyTaskMapper.insert(task);

            createdTasks.add(toResponse(task));
        }

        studyTaskMapper.insertLearningHistory(
                currentUserId,
                courseId,
                "REVIEW",
                "COURSE",
                courseId,
                null,
                now
        );

        return new GenerateStudyTasksResponse(
                courseId,
                createdTasks.size(),
                "Study checklist generated successfully.",
                createdTasks
        );
    }

    public List<StudyTaskResponse> getCourseTasks(Long currentUserId,
                                                  Long courseId) {
        validateCurrentUser(currentUserId);
        validateCourseId(courseId);

        ensureCourseAccess(currentUserId, courseId);

        return studyTaskMapper
                .findByCourseIdAndUserId(currentUserId, courseId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public StudyTaskResponse updateTask(Long currentUserId,
                                        Long taskId,
                                        UpdateStudyTaskRequest request) {
        validateCurrentUser(currentUserId);
        validateTaskId(taskId);

        if (request == null) {
            throw new BusinessException(
                    "INVALID_TASK_REQUEST",
                    "Task update request is required."
            );
        }

        StudyTask existingTask = studyTaskMapper.findByIdAndUserId(
                taskId,
                currentUserId
        );

        if (existingTask == null) {
            throw new BusinessException(
                    "TASK_NOT_FOUND",
                    "Task not found or access denied."
            );
        }

        if (request.getTitle() != null) {
            if (request.getTitle().isBlank()) {
                throw new BusinessException(
                        "INVALID_TASK_TITLE",
                        "Task title cannot be blank."
                );
            }

            existingTask.setTitle(request.getTitle().trim());
        }

        if (request.getDescription() != null) {
            existingTask.setDescription(normalizeBlankToNull(request.getDescription()));
        }

        if (request.getDueDate() != null) {
            existingTask.setDueDate(request.getDueDate());
        }

        existingTask.setUpdatedAt(LocalDateTime.now());

        int updated = studyTaskMapper.update(existingTask);

        if (updated == 0) {
            throw new BusinessException(
                    "TASK_UPDATE_FAILED",
                    "Failed to update task."
            );
        }

        return toResponse(existingTask);
    }

    public StudyTaskResponse completeTask(Long currentUserId,
                                          Long taskId) {
        validateCurrentUser(currentUserId);
        validateTaskId(taskId);

        StudyTask existingTask = studyTaskMapper.findByIdAndUserId(
                taskId,
                currentUserId
        );

        if (existingTask == null) {
            throw new BusinessException(
                    "TASK_NOT_FOUND",
                    "Task not found or access denied."
            );
        }

        LocalDateTime now = LocalDateTime.now();

        int updated = studyTaskMapper.markDone(
                taskId,
                currentUserId,
                now
        );

        if (updated == 0) {
            throw new BusinessException(
                    "TASK_UPDATE_FAILED",
                    "Failed to complete task."
            );
        }

        existingTask.setStatus(StudyTaskStatus.DONE.name());
        existingTask.setUpdatedAt(now);

        return toResponse(existingTask);
    }

    public void deleteTask(Long currentUserId,
                           Long taskId) {
        validateCurrentUser(currentUserId);
        validateTaskId(taskId);

        int deleted = studyTaskMapper.deleteByIdAndUserId(
                taskId,
                currentUserId
        );

        if (deleted == 0) {
            throw new BusinessException(
                    "TASK_NOT_FOUND",
                    "Task not found or access denied."
            );
        }
    }

    private void validateCurrentUser(Long currentUserId) {
        if (currentUserId == null) {
            throw new BusinessException(
                    "UNAUTHORIZED",
                    "Current user is required."
            );
        }
    }

    private void validateCourseId(Long courseId) {
        if (courseId == null) {
            throw new BusinessException(
                    "INVALID_COURSE_ID",
                    "Course id is required."
            );
        }
    }

    private void validateTaskId(Long taskId) {
        if (taskId == null) {
            throw new BusinessException(
                    "INVALID_TASK_ID",
                    "Task id is required."
            );
        }
    }

    private void validateCreateRequest(CreateStudyTaskRequest request) {
        if (request == null) {
            throw new BusinessException(
                    "INVALID_TASK_REQUEST",
                    "Task request is required."
            );
        }

        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new BusinessException(
                    "INVALID_TASK_TITLE",
                    "Task title is required."
            );
        }
    }

    private void ensureCourseAccess(Long userId, Long courseId) {
        int count = studyTaskMapper.countCourseOwnership(userId, courseId);

        if (count == 0) {
            throw new BusinessException(
                    "COURSE_NOT_FOUND",
                    "Course not found or access denied."
            );
        }
    }

    private void ensureDocumentAccess(Long userId,
                                      Long courseId,
                                      Long documentId) {
        int count = studyTaskMapper.countDocumentOwnership(
                userId,
                courseId,
                documentId
        );

        if (count == 0) {
            throw new BusinessException(
                    "DOCUMENT_NOT_FOUND",
                    "Document not found or access denied."
            );
        }
    }

    private String normalizeBlankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    private StudyTaskResponse toResponse(StudyTask task) {
        return new StudyTaskResponse(
                task.getId(),
                task.getCourseId(),
                task.getDocumentId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getDueDate(),
                task.getSourceType(),
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }

    private List<TaskCandidate> buildAssignmentTaskCandidates(AssignmentAnalysis analysis,
                                                              LocalDateTime fallbackDueDate) {
        List<TaskCandidate> candidates = new ArrayList<>();

        LocalDateTime dueDate = analysis.getDeadline() != null
                ? analysis.getDeadline()
                : fallbackDueDate;

        List<String> checklist = fromStringListJson(analysis.getChecklistJson());
        List<String> deliverables = fromStringListJson(analysis.getDeliverablesJson());
        List<String> requirements = fromStringListJson(analysis.getRequirementsJson());

        for (String item : checklist) {
            if (item == null || item.isBlank()) {
                continue;
            }

            candidates.add(new TaskCandidate(
                    analysis.getDocumentId(),
                    item.trim(),
                    "Generated from assignment checklist.",
                    dueDate,
                    StudyTaskSourceType.ASSIGNMENT.name()
            ));
        }

        for (String deliverable : deliverables) {
            if (deliverable == null || deliverable.isBlank()) {
                continue;
            }

            candidates.add(new TaskCandidate(
                    analysis.getDocumentId(),
                    "Prepare deliverable: " + deliverable.trim(),
                    "Make sure this assignment deliverable is completed and ready for submission.",
                    dueDate,
                    StudyTaskSourceType.ASSIGNMENT.name()
            ));
        }

        for (String requirement : requirements) {
            if (requirement == null || requirement.isBlank()) {
                continue;
            }

            candidates.add(new TaskCandidate(
                    analysis.getDocumentId(),
                    "Check requirement: " + requirement.trim(),
                    "Verify that this requirement is clearly addressed in your work.",
                    dueDate,
                    StudyTaskSourceType.ASSIGNMENT.name()
            ));
        }

        return candidates;
    }

    private List<TaskCandidate> buildRubricTaskCandidates(RubricAnalysis analysis,
                                                          LocalDateTime fallbackDueDate) {
        List<TaskCandidate> candidates = new ArrayList<>();

        List<RubricCriterionResult> criteria = fromCriteriaJson(analysis.getCriteriaJson());
        List<String> excellentBand = fromStringListJson(analysis.getExcellentBandJson());

        for (RubricCriterionResult criterion : criteria) {
            if (criterion == null || criterion.getName() == null || criterion.getName().isBlank()) {
                continue;
            }

            String weight = criterion.getWeight() == null || criterion.getWeight().isBlank()
                    ? "Not specified"
                    : criterion.getWeight();

            String description = criterion.getDescription() == null
                    ? ""
                    : criterion.getDescription();

            candidates.add(new TaskCandidate(
                    analysis.getDocumentId(),
                    "Address rubric criterion: " + criterion.getName().trim(),
                    "Weight: " + weight + ". " + description,
                    fallbackDueDate,
                    StudyTaskSourceType.RUBRIC.name()
            ));
        }

        for (String excellentItem : excellentBand) {
            if (excellentItem == null || excellentItem.isBlank()) {
                continue;
            }

            candidates.add(new TaskCandidate(
                    analysis.getDocumentId(),
                    "Target excellent band: " + shortenTitle(excellentItem.trim()),
                    excellentItem.trim(),
                    fallbackDueDate,
                    StudyTaskSourceType.RUBRIC.name()
            ));
        }

        if (analysis.getHighScoreStrategy() != null && !analysis.getHighScoreStrategy().isBlank()) {
            candidates.add(new TaskCandidate(
                    analysis.getDocumentId(),
                    "Apply high score strategy from rubric",
                    analysis.getHighScoreStrategy(),
                    fallbackDueDate,
                    StudyTaskSourceType.RUBRIC.name()
            ));
        }

        return candidates;
    }

    private List<TaskCandidate> deduplicateAndLimit(List<TaskCandidate> candidates,
                                                    int maxTasks) {
        List<TaskCandidate> result = new ArrayList<>();
        Set<String> seenTitles = new LinkedHashSet<>();

        for (TaskCandidate candidate : candidates) {
            if (candidate == null || candidate.title() == null || candidate.title().isBlank()) {
                continue;
            }

            String normalizedTitle = candidate.title().trim();

            if (!seenTitles.add(normalizedTitle)) {
                continue;
            }

            result.add(candidate);

            if (result.size() >= maxTasks) {
                break;
            }
        }

        return result;
    }

    private int normalizeMaxTasks(Integer maxTasks) {
        if (maxTasks == null || maxTasks <= 0) {
            return 20;
        }

        return Math.min(maxTasks, 50);
    }

    private String shortenTitle(String text) {
        if (text == null) {
            return "";
        }

        if (text.length() <= 80) {
            return text;
        }

        return text.substring(0, 80) + "...";
    }

    private List<String> fromStringListJson(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }

        try {
            return objectMapper.readValue(
                    json,
                    new TypeReference<List<String>>() {
                    }
            );

        } catch (JsonProcessingException e) {
            return List.of();
        }
    }

    private List<RubricCriterionResult> fromCriteriaJson(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }

        try {
            return objectMapper.readValue(
                    json,
                    new TypeReference<List<RubricCriterionResult>>() {
                    }
            );

        } catch (JsonProcessingException e) {
            return List.of();
        }
    }

    private record TaskCandidate(
            Long documentId,
            String title,
            String description,
            LocalDateTime dueDate,
            String sourceType
    ) {
    }
}