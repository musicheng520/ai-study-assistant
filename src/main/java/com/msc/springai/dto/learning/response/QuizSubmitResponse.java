package com.msc.springai.dto.learning.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class QuizSubmitResponse {

    private Long attemptId;

    private Long quizId;

    private Double score;

    private Integer totalQuestions;

    private Integer correctCount;

    private List<WrongAnswerResponse> wrongAnswers;
}