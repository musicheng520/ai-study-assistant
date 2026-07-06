package com.msc.springai.controller;

import com.msc.springai.dto.course.CourseCreateRequest;
import com.msc.springai.dto.course.CourseDashboardResponse;
import com.msc.springai.dto.course.CourseResponse;
import com.msc.springai.dto.course.CourseUpdateRequest;
import com.msc.springai.entity.Course;
import com.msc.springai.service.CourseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {

    private static final Long DEV_USER_ID = 1L;

    private final CourseService courseService;

    @PostMapping
    public CourseResponse createCourse(@Valid @RequestBody CourseCreateRequest request) {
        Course course = courseService.createCourse(DEV_USER_ID, request);
        return CourseResponse.from(course);
    }

    @GetMapping
    public List<CourseResponse> getMyCourses() {
        return courseService.findByUserId(DEV_USER_ID)
                .stream()
                .map(CourseResponse::from)
                .toList();
    }

    @GetMapping("/{courseId}")
    public CourseResponse getCourseById(@PathVariable Long courseId) {
        Course course = courseService.findByIdAndCheckOwner(courseId, DEV_USER_ID);
        return CourseResponse.from(course);
    }

    @PutMapping("/{courseId}")
    public CourseResponse updateCourse(@PathVariable Long courseId,
                                       @Valid @RequestBody CourseUpdateRequest request) {
        Course course = courseService.updateCourse(courseId, DEV_USER_ID, request);
        return CourseResponse.from(course);
    }

    @DeleteMapping("/{courseId}")
    public ResponseEntity<Void> deleteCourse(@PathVariable Long courseId) {
        courseService.deleteCourse(courseId, DEV_USER_ID);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{courseId}/dashboard")
    public CourseDashboardResponse getCourseDashboard(@PathVariable Long courseId) {
        Course course = courseService.findByIdAndCheckOwner(courseId, DEV_USER_ID);

        return new CourseDashboardResponse(
                course.getId(),
                course.getName(),
                0,
                0,
                0,
                0,
                "Dashboard API is ready. Document, chat, quiz and flashcard counts will be added later."
        );
    }
}