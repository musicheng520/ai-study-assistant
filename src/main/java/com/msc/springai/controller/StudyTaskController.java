package com.msc.springai.controller;

import com.msc.springai.dto.workflow.task.*;
import com.msc.springai.security.CurrentUserUtil;
import com.msc.springai.service.StudyTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class StudyTaskController {

    private final StudyTaskService studyTaskService;

    @PostMapping("/courses/{courseId}/tasks")
    public StudyTaskResponse createTask(@PathVariable Long courseId,
                                        @RequestBody CreateStudyTaskRequest request) {
        Long currentUserId = CurrentUserUtil.getCurrentUserId();

        return studyTaskService.createManualTask(
                currentUserId,
                courseId,
                request
        );
    }

    @PostMapping("/courses/{courseId}/tasks/generate")
    public GenerateStudyTasksResponse generateTasks(
            @PathVariable Long courseId,
            @RequestBody(required = false) GenerateStudyTasksRequest request
    ) {
        Long currentUserId = CurrentUserUtil.getCurrentUserId();

        return studyTaskService.generateTasks(
                currentUserId,
                courseId,
                request
        );
    }

    @GetMapping("/courses/{courseId}/tasks")
    public List<StudyTaskResponse> getCourseTasks(@PathVariable Long courseId) {
        Long currentUserId = CurrentUserUtil.getCurrentUserId();

        return studyTaskService.getCourseTasks(
                currentUserId,
                courseId
        );
    }

    @PutMapping("/tasks/{taskId}")
    public StudyTaskResponse updateTask(@PathVariable Long taskId,
                                        @RequestBody UpdateStudyTaskRequest request) {
        Long currentUserId = CurrentUserUtil.getCurrentUserId();

        return studyTaskService.updateTask(
                currentUserId,
                taskId,
                request
        );
    }

    @PatchMapping("/tasks/{taskId}/complete")
    public StudyTaskResponse completeTask(@PathVariable Long taskId) {
        Long currentUserId = CurrentUserUtil.getCurrentUserId();

        return studyTaskService.completeTask(
                currentUserId,
                taskId
        );
    }

    @DeleteMapping("/tasks/{taskId}")
    public void deleteTask(@PathVariable Long taskId) {
        Long currentUserId = CurrentUserUtil.getCurrentUserId();

        studyTaskService.deleteTask(
                currentUserId,
                taskId
        );
    }
}