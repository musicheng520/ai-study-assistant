package com.msc.springai.controller;

import com.msc.springai.entity.Course;
import com.msc.springai.entity.User;
import com.msc.springai.service.CourseService;
import com.msc.springai.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dev")
@RequiredArgsConstructor
public class DevController {

    private final UserService userService;
    private final CourseService courseService;

    @PostMapping("/seed")
    public Map<String, Object> seed() {
        User user = userService.createUserIfNotExists(
                "test@example.com",
                "Test Student"
        );

        Course course = courseService.createCourse(
                user.getId(),
                "AI Systems",
                "AI101"
        );

        return Map.of(
                "message", "seed data created",
                "user", user,
                "course", course
        );
    }

    @GetMapping("/users/{id}")
    public User getUser(@PathVariable Long id) {
        return userService.findById(id);
    }

    @GetMapping("/users/{userId}/courses")
    public List<Course> getUserCourses(@PathVariable Long userId) {
        return courseService.findByUserId(userId);
    }
}