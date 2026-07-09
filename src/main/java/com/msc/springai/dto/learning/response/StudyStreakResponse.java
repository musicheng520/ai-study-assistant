package com.msc.springai.dto.learning.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StudyStreakResponse {

    private Integer currentStreak;

    private Integer longestStreak;

    private LocalDate lastActivityDate;
}