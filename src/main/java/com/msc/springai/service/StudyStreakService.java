package com.msc.springai.service;

import com.msc.springai.entity.StudyStreak;
import com.msc.springai.mapper.StudyStreakMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class StudyStreakService {

    private final StudyStreakMapper studyStreakMapper;

    public void updateStreak(Long userId) {
        LocalDate today = LocalDate.now();

        StudyStreak streak = studyStreakMapper.findByUserId(userId);

        if (streak == null) {
            StudyStreak newStreak = new StudyStreak();
            newStreak.setUserId(userId);
            newStreak.setCurrentStreak(1);
            newStreak.setLongestStreak(1);
            newStreak.setLastActivityDate(today);

            studyStreakMapper.insert(newStreak);
            return;
        }

        LocalDate lastActivityDate = streak.getLastActivityDate();

        if (today.equals(lastActivityDate)) {
            return;
        }

        int newCurrentStreak;

        if (today.minusDays(1).equals(lastActivityDate)) {
            newCurrentStreak = streak.getCurrentStreak() + 1;
        } else {
            newCurrentStreak = 1;
        }

        int newLongestStreak = Math.max(
                streak.getLongestStreak(),
                newCurrentStreak
        );

        streak.setCurrentStreak(newCurrentStreak);
        streak.setLongestStreak(newLongestStreak);
        streak.setLastActivityDate(today);

        studyStreakMapper.update(streak);
    }
}