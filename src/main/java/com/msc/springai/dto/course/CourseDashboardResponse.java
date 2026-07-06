package com.msc.springai.dto.course;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CourseDashboardResponse {

    private Long courseId;

    private String courseName;

    private int documentCount;

    private int chatCount;

    private int quizCount;

    private int flashcardCount;

    private String message;
}