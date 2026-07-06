package com.msc.springai.service;

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

    public Course createCourse(Long userId, String name, String code) {
        Course course = new Course();
        course.setUserId(userId);
        course.setName(name);
        course.setCode(code);
        course.setDescription("Test course created from dev endpoint");
        course.setColor("#4F46E5");
        course.setProgressScore(BigDecimal.ZERO);

        courseMapper.insert(course);
        return course;
    }

    public Course findById(Long id) {
        return courseMapper.findById(id);
    }

    public List<Course> findByUserId(Long userId) {
        return courseMapper.findByUserId(userId);
    }
}