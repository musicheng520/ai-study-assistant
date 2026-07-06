package com.msc.springai.controller;

import com.msc.springai.dto.course.CourseCreateRequest;
import com.msc.springai.dto.course.CourseResponse;
import com.msc.springai.entity.Course;
import com.msc.springai.service.CourseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
}