package com.msc.springai.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AiFeedback {

    private Long id;

    private Long userId;

    private Long courseId;

    private String targetType;

    private Long targetId;

    private String rating;

    private String comment;

    private LocalDateTime createdAt;
}