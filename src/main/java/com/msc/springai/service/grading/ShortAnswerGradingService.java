package com.msc.springai.service.grading;

import org.springframework.stereotype.Service;

@Service
public class ShortAnswerGradingService {

    public boolean grade(
            String userAnswer,
            String correctAnswer,
            String explanation
    ) {
        return userAnswer != null && !userAnswer.trim().isEmpty();
    }
}