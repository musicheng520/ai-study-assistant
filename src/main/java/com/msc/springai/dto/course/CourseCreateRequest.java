package com.msc.springai.dto.course;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CourseCreateRequest {

    @NotBlank(message = "Course name is required")
    @Size(max = 150, message = "Course name must be less than 150 characters")
    private String name;

    @Size(max = 50, message = "Course code must be less than 50 characters")
    private String code;

    private String description;

    @Size(max = 30, message = "Color must be less than 30 characters")
    private String color;
}