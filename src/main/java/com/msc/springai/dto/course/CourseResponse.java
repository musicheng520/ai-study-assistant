package com.msc.springai.dto.course;

import com.msc.springai.entity.Course;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CourseResponse {

    private Long id;
    private Long userId;
    private String name;
    private String code;
    private String description;
    private String color;
    private BigDecimal progressScore;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static CourseResponse from(Course course) {
        CourseResponse response = new CourseResponse();
        response.setId(course.getId());
        response.setUserId(course.getUserId());
        response.setName(course.getName());
        response.setCode(course.getCode());
        response.setDescription(course.getDescription());
        response.setColor(course.getColor());
        response.setProgressScore(course.getProgressScore());
        response.setCreatedAt(course.getCreatedAt());
        response.setUpdatedAt(course.getUpdatedAt());
        return response;
    }
}