package com.msc.springai.service;

import com.msc.springai.dto.workflow.task.CreateStudyTaskRequest;
import com.msc.springai.dto.workflow.task.StudyTaskResponse;
import com.msc.springai.dto.workflow.task.UpdateStudyTaskRequest;
import com.msc.springai.entity.StudyTask;
import com.msc.springai.entity.StudyTaskSourceType;
import com.msc.springai.entity.StudyTaskStatus;
import com.msc.springai.exception.BusinessException;
import com.msc.springai.mapper.StudyTaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StudyTaskService {

    private final StudyTaskMapper studyTaskMapper;

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
}