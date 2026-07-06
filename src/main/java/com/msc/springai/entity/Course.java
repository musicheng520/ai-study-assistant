package com.msc.springai.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class Course {

    private Long id;

    private Long userId;

    private String name;

    private String code;

    private String description;

    private String color;

    private BigDecimal progressScore;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}