package com.msc.springai.service;

import com.msc.springai.dto.course.CourseCreateRequest;
import com.msc.springai.dto.course.CourseUpdateRequest;
import com.msc.springai.entity.Course;
import com.msc.springai.mapper.CourseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseMapper courseMapper;

    public Course createCourse(Long userId, CourseCreateRequest request) {
        Course course = new Course();
        course.setUserId(userId);
        course.setName(request.getName());
        course.setCode(request.getCode());
        course.setDescription(request.getDescription());
        course.setColor(request.getColor() == null || request.getColor().isBlank()
                ? "#4F46E5"
                : request.getColor());
        course.setProgressScore(BigDecimal.ZERO);

        courseMapper.insert(course);
        return courseMapper.findById(course.getId());
    }

    public Course createCourse(Long userId, String name, String code) {
        Course course = new Course();
        course.setUserId(userId);
        course.setName(name);
        course.setCode(code);
        course.setDescription("Test course created from dev endpoint");
        course.setColor("#4F46E5");
        course.setProgressScore(BigDecimal.ZERO);

        courseMapper.insert(course);
        return courseMapper.findById(course.getId());
    }

    public Course findById(Long id) {
        return courseMapper.findById(id);
    }

    public Course findByIdAndCheckOwner(Long courseId, Long userId) {
        Course course = courseMapper.findById(courseId);

        if (course == null) {
            throw new IllegalArgumentException("Course not found");
        }

        if (!course.getUserId().equals(userId)) {
            throw new IllegalArgumentException("You do not have permission to access this course");
        }

        return course;
    }

    public List<Course> findByUserId(Long userId) {
        return courseMapper.findByUserId(userId);
    }

    public Course updateCourse(Long courseId, Long userId, CourseUpdateRequest request) {
        Course existingCourse = findByIdAndCheckOwner(courseId, userId);

        existingCourse.setName(request.getName());
        existingCourse.setCode(request.getCode());
        existingCourse.setDescription(request.getDescription());
        existingCourse.setColor(request.getColor() == null || request.getColor().isBlank()
                ? existingCourse.getColor()
                : request.getColor());

        int rows = courseMapper.update(existingCourse);
        if (rows == 0) {
            throw new IllegalArgumentException("Course update failed");
        }

        return courseMapper.findById(courseId);
    }

    public void deleteCourse(Long courseId, Long userId) {
        findByIdAndCheckOwner(courseId, userId);

        int rows = courseMapper.deleteByIdAndUserId(courseId, userId);
        if (rows == 0) {
            throw new IllegalArgumentException("Course delete failed");
        }
    }
}