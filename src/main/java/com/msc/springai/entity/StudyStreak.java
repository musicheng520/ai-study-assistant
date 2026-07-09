package com.msc.springai.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StudyStreak {

    private Long id;

    private Long userId;

    private Integer currentStreak;

    private Integer longestStreak;

    private LocalDate lastActivityDate;

    private LocalDateTime updatedAt;
}