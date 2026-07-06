package com.msc.springai.controller;

import com.msc.springai.dto.course.CourseCreateRequest;
import com.msc.springai.dto.course.CourseDashboardResponse;
import com.msc.springai.dto.course.CourseResponse;
import com.msc.springai.dto.course.CourseUpdateRequest;
import com.msc.springai.entity.Course;
import com.msc.springai.security.CurrentUserUtil;
import com.msc.springai.service.CourseService;
import com.msc.springai.service.DocumentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;
    private final DocumentService documentService;

    @PostMapping
    public CourseResponse createCourse(@Valid @RequestBody CourseCreateRequest request) {
        Long currentUserId = CurrentUserUtil.getCurrentUserId();
        Course course = courseService.createCourse(currentUserId, request);
        return CourseResponse.from(course);
    }

    @GetMapping
    public List<CourseResponse> getMyCourses() {
        Long currentUserId = CurrentUserUtil.getCurrentUserId();

        return courseService.findByUserId(currentUserId)
                .stream()
                .map(CourseResponse::from)
                .toList();
    }

    @GetMapping("/{courseId}")
    public CourseResponse getCourseById(@PathVariable Long courseId) {
        Long currentUserId = CurrentUserUtil.getCurrentUserId();
        Course course = courseService.findByIdAndCheckOwner(courseId, currentUserId);
        return CourseResponse.from(course);
    }

    @PutMapping("/{courseId}")
    public CourseResponse updateCourse(@PathVariable Long courseId,
                                       @Valid @RequestBody CourseUpdateRequest request) {
        Long currentUserId = CurrentUserUtil.getCurrentUserId();
        Course course = courseService.updateCourse(courseId, currentUserId, request);
        return CourseResponse.from(course);
    }

    @DeleteMapping("/{courseId}")
    public ResponseEntity<Void> deleteCourse(@PathVariable Long courseId) {
        Long currentUserId = CurrentUserUtil.getCurrentUserId();
        courseService.deleteCourse(courseId, currentUserId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{courseId}/dashboard")
    public CourseDashboardResponse getCourseDashboard(@PathVariable Long courseId) {
        Long currentUserId = CurrentUserUtil.getCurrentUserId();
        Course course = courseService.findByIdAndCheckOwner(courseId, currentUserId);

        int documentCount = documentService.countCourseDocuments(currentUserId, courseId);

        return new CourseDashboardResponse(
                course.getId(),
                course.getName(),
                documentCount,
                0,
                0,
                0,
                "Dashboard API is ready. Chat, quiz and flashcard counts will be added later."
        );
    }
}