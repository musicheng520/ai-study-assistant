package com.msc.springai.dto.feedback;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AiFeedbackResponse {

    private Long id;

    private Long courseId;

    private String targetType;

    private Long targetId;

    private String rating;

    private String comment;

    private LocalDateTime createdAt;
}