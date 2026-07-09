package com.msc.springai.service;

import com.msc.springai.dto.learning.request.SubmitQuizRequest;
import com.msc.springai.dto.learning.response.QuizAttemptResponse;
import com.msc.springai.dto.learning.response.QuizSubmitResponse;
import com.msc.springai.dto.learning.response.WrongAnswerResponse;
import com.msc.springai.entity.Quiz;
import com.msc.springai.entity.QuizAttempt;
import com.msc.springai.entity.QuizAttemptAnswer;
import com.msc.springai.entity.QuizQuestion;
import com.msc.springai.entity.WrongAnswer;
import com.msc.springai.exception.BusinessException;
import com.msc.springai.mapper.LearningHistoryMapper;
import com.msc.springai.mapper.QuizAttemptAnswerMapper;
import com.msc.springai.mapper.QuizAttemptMapper;
import com.msc.springai.mapper.QuizMapper;
import com.msc.springai.mapper.QuizQuestionMapper;
import com.msc.springai.mapper.WrongAnswerMapper;
import com.msc.springai.service.grading.ShortAnswerGradingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuizSubmitService {

    private static final String QUESTION_TYPE_MCQ = "MCQ";
    private static final String QUESTION_TYPE_SHORT_ANSWER = "SHORT_ANSWER";

    private final QuizMapper quizMapper;
    private final QuizQuestionMapper quizQuestionMapper;
    private final QuizAttemptMapper quizAttemptMapper;
    private final QuizAttemptAnswerMapper quizAttemptAnswerMapper;
    private final WrongAnswerMapper wrongAnswerMapper;
    private final LearningHistoryMapper learningHistoryMapper;
    private final StudyStreakService studyStreakService;
    private final ShortAnswerGradingService shortAnswerGradingService;

    @Transactional
    public QuizSubmitResponse submitQuiz(
            Long userId,
            Long quizId,
            SubmitQuizRequest request
    ) {
        if (request == null ||
                request.getAnswers() == null ||
                request.getAnswers().isEmpty()) {
            throw new BusinessException(
                    "ANSWERS_REQUIRED",
                    "Answers are required."
            );
        }

        Quiz quiz = quizMapper.findByIdAndUserId(
                quizId,
                userId
        );

        if (quiz == null) {
            throw new BusinessException(
                    "QUIZ_NOT_FOUND",
                    "Quiz not found or access denied."
            );
        }

        List<QuizQuestion> questions = quizQuestionMapper.findByQuizId(quizId);

        if (questions == null || questions.isEmpty()) {
            throw new BusinessException(
                    "QUIZ_HAS_NO_QUESTIONS",
                    "This quiz has no questions."
            );
        }

        Map<Long, QuizQuestion> questionMap = questions.stream()
                .collect(Collectors.toMap(
                        QuizQuestion::getId,
                        Function.identity()
                ));

        Map<Long, String> answerMap = buildAnswerMap(
                request,
                questionMap
        );

        int totalQuestions = questions.size();
        int correctCount = 0;

        List<WrongAnswerResponse> wrongAnswerResponses = new ArrayList<>();

        QuizAttempt attempt = new QuizAttempt();
        attempt.setUserId(userId);
        attempt.setCourseId(quiz.getCourseId());
        attempt.setQuizId(quizId);
        attempt.setStartedAt(LocalDateTime.now());
        attempt.setSubmittedAt(LocalDateTime.now());
        attempt.setTotalQuestions(totalQuestions);
        attempt.setCorrectCount(0);
        attempt.setScore(0.0);

        quizAttemptMapper.insert(attempt);

        for (QuizQuestion question : questions) {
            String userAnswer = answerMap.get(question.getId());

            boolean isCorrect = gradeQuestion(
                    question,
                    userAnswer
            );

            if (isCorrect) {
                correctCount++;
            }

            QuizAttemptAnswer attemptAnswer = new QuizAttemptAnswer();
            attemptAnswer.setAttemptId(attempt.getId());
            attemptAnswer.setQuestionId(question.getId());
            attemptAnswer.setUserAnswer(userAnswer);
            attemptAnswer.setIsCorrect(isCorrect);

            quizAttemptAnswerMapper.insert(attemptAnswer);

            if (!isCorrect) {
                WrongAnswer wrongAnswer = buildWrongAnswer(
                        userId,
                        quiz,
                        question,
                        userAnswer
                );

                wrongAnswerMapper.insert(wrongAnswer);

                wrongAnswerResponses.add(new WrongAnswerResponse(
                        question.getId(),
                        wrongAnswer.getTopic(),
                        userAnswer,
                        question.getCorrectAnswer(),
                        question.getExplanation()
                ));
            }
        }

        double score = calculateScore(
                correctCount,
                totalQuestions
        );

        attempt.setCorrectCount(correctCount);
        attempt.setScore(score);

        /*
         * 因为 attempt 先 insert 是为了拿到 attemptId。
         * 所以后面补一次 update score。
         */
        updateAttemptScore(
                attempt.getId(),
                correctCount,
                score
        );

        learningHistoryMapper.insertLearningHistory(
                userId,
                quiz.getCourseId(),
                "QUIZ",
                "QUIZ",
                quizId,
                quiz.getTitle()
        );

        studyStreakService.updateStreak(userId);

        return new QuizSubmitResponse(
                attempt.getId(),
                quizId,
                score,
                totalQuestions,
                correctCount,
                wrongAnswerResponses
        );
    }

    public List<QuizAttemptResponse> getQuizAttempts(
            Long userId,
            Long quizId
    ) {
        Quiz quiz = quizMapper.findByIdAndUserId(
                quizId,
                userId
        );

        if (quiz == null) {
            throw new BusinessException(
                    "QUIZ_NOT_FOUND",
                    "Quiz not found or access denied."
            );
        }

        return quizAttemptMapper.findByUserIdAndQuizId(
                        userId,
                        quizId
                )
                .stream()
                .map(this::toAttemptResponse)
                .toList();
    }

    private Map<Long, String> buildAnswerMap(
            SubmitQuizRequest request,
            Map<Long, QuizQuestion> questionMap
    ) {
        Map<Long, String> answerMap = new HashMap<>();

        for (SubmitQuizRequest.AnswerItem item : request.getAnswers()) {
            if (item.getQuestionId() == null) {
                throw new BusinessException(
                        "QUESTION_ID_REQUIRED",
                        "Question id is required."
                );
            }

            if (!questionMap.containsKey(item.getQuestionId())) {
                throw new BusinessException(
                        "QUESTION_NOT_IN_QUIZ",
                        "Submitted question does not belong to this quiz."
                );
            }

            if (answerMap.containsKey(item.getQuestionId())) {
                throw new BusinessException(
                        "DUPLICATE_QUESTION_ANSWER",
                        "Duplicate answer for the same question."
                );
            }

            answerMap.put(
                    item.getQuestionId(),
                    item.getAnswer()
            );
        }

        return answerMap;
    }

    private boolean gradeQuestion(
            QuizQuestion question,
            String userAnswer
    ) {
        String questionType = question.getQuestionType();

        if (QUESTION_TYPE_MCQ.equalsIgnoreCase(questionType)) {
            return gradeMcq(
                    userAnswer,
                    question.getCorrectAnswer()
            );
        }

        if (QUESTION_TYPE_SHORT_ANSWER.equalsIgnoreCase(questionType)) {
            return shortAnswerGradingService.grade(
                    userAnswer,
                    question.getCorrectAnswer(),
                    question.getExplanation()
            );
        }

        return false;
    }

    private boolean gradeMcq(
            String userAnswer,
            String correctAnswer
    ) {
        if (userAnswer == null || correctAnswer == null) {
            return false;
        }

        String normalizedUserAnswer = normalizeMcqAnswer(userAnswer);
        String normalizedCorrectAnswer = normalizeMcqAnswer(correctAnswer);

        return normalizedUserAnswer.equalsIgnoreCase(normalizedCorrectAnswer);
    }

    private String normalizeMcqAnswer(String answer) {
        String cleaned = answer.trim();

        if (cleaned.isEmpty()) {
            return "";
        }

        /*
         * 支持这些格式：
         * B
         * b
         * B.
         * B. Analysers
         * B) Analysers
         * B - Analysers
         */
        char firstChar = cleaned.charAt(0);

        if (Character.isLetter(firstChar)) {
            return String.valueOf(Character.toUpperCase(firstChar));
        }

        return cleaned.toUpperCase();
    }

    private double calculateScore(
            int correctCount,
            int totalQuestions
    ) {
        if (totalQuestions <= 0) {
            return 0.0;
        }

        return Math.round(
                correctCount * 10000.0 / totalQuestions
        ) / 100.0;
    }

    private WrongAnswer buildWrongAnswer(
            Long userId,
            Quiz quiz,
            QuizQuestion question,
            String userAnswer
    ) {
        WrongAnswer wrongAnswer = new WrongAnswer();

        wrongAnswer.setUserId(userId);
        wrongAnswer.setCourseId(quiz.getCourseId());
        wrongAnswer.setQuizId(quiz.getId());
        wrongAnswer.setQuestionId(question.getId());
        wrongAnswer.setTopic(normalizeTopic(question.getTopic()));
        wrongAnswer.setUserAnswer(userAnswer);
        wrongAnswer.setCorrectAnswer(question.getCorrectAnswer());
        wrongAnswer.setExplanation(question.getExplanation());
        wrongAnswer.setResolved(false);

        return wrongAnswer;
    }

    private String normalizeTopic(String topic) {
        if (topic == null || topic.isBlank()) {
            return "General";
        }

        return topic.trim();
    }

    private QuizAttemptResponse toAttemptResponse(
            QuizAttempt attempt
    ) {
        return new QuizAttemptResponse(
                attempt.getId(),
                attempt.getQuizId(),
                attempt.getScore(),
                attempt.getTotalQuestions(),
                attempt.getCorrectCount(),
                attempt.getStartedAt(),
                attempt.getSubmittedAt()
        );
    }

    private void updateAttemptScore(
            Long attemptId,
            Integer correctCount,
            Double score
    ) {
        quizAttemptMapper.updateScore(
                attemptId,
                correctCount,
                score
        );
    }
}