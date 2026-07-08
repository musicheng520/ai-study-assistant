package com.msc.springai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.msc.springai.dto.learning.draft.QuizDraftValue;
import com.msc.springai.dto.learning.request.QuizGenerateRequest;
import com.msc.springai.dto.learning.request.SaveDraftRequest;
import com.msc.springai.dto.learning.response.*;
import com.msc.springai.dto.learning.result.QuizQuestionResult;
import com.msc.springai.dto.learning.result.QuizResult;
import com.msc.springai.dto.learning.retrieval.RetrievedChunk;
import com.msc.springai.entity.Course;
import com.msc.springai.entity.CourseDocument;
import com.msc.springai.entity.Quiz;
import com.msc.springai.entity.QuizQuestion;
import com.msc.springai.exception.BusinessException;
import com.msc.springai.mapper.*;
import com.msc.springai.service.prompt.QuizPromptBuilder;
import com.msc.springai.service.validator.QuizOutputValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.*;

@Service
@RequiredArgsConstructor
public class QuizService {

    private static final String SCOPE_COURSE = "COURSE";
    private static final String SCOPE_DOCUMENT = "DOCUMENT";

    private final RetrievalService retrievalService;
    private final DraftCacheService draftCacheService;
    private final QuizOutputValidator quizOutputValidator;
    private final QuizPromptBuilder quizPromptBuilder;

    private final QuizMapper quizMapper;
    private final QuizQuestionMapper quizQuestionMapper;
    private final CourseMapper courseMapper;
    private final CourseDocumentMapper courseDocumentMapper;
    private final LearningHistoryMapper learningHistoryMapper;

    private final ObjectMapper objectMapper;
    private final ChatClient.Builder chatClientBuilder;

    public QuizGenerateResponse generateCourseQuiz(
            Long userId,
            Long courseId,
            QuizGenerateRequest request
    ) {
        QuizGenerateOptions options = normalizeOptions(request);

        List<RetrievedChunk> chunks = retrievalService.retrieveCourseChunks(
                userId,
                courseId,
                options.topK(),
                options.retrievalQuery()
        );

        QuizResult result = generateQuizFromChunks(
                SCOPE_COURSE,
                options.difficulty(),
                options.mcqCount(),
                options.shortAnswerCount(),
                chunks
        );

        QuizDraftValue draftValue = new QuizDraftValue(
                userId,
                courseId,
                null,
                SCOPE_COURSE,
                options.mcqCount(),
                options.shortAnswerCount(),
                options.difficulty(),
                result
        );

        Map<String, Object> params = new LinkedHashMap<>();
        params.put("documentId", null);
        params.put("topK", options.topK());
        params.put("retrievalQuery", options.retrievalQuery());
        params.put("mcqCount", options.mcqCount());
        params.put("shortAnswerCount", options.shortAnswerCount());
        params.put("difficulty", options.difficulty());

        String draftKey = draftCacheService.buildQuizDraftKey(
                userId,
                courseId,
                SCOPE_COURSE,
                params
        );

        draftCacheService.saveDraft(
                draftKey,
                draftValue,
                Duration.ofDays(7)
        );

        return toGenerateResponse(
                draftKey,
                SCOPE_COURSE,
                result
        );
    }

    public QuizGenerateResponse generateDocumentQuiz(
            Long userId,
            Long documentId,
            QuizGenerateRequest request
    ) {
        CourseDocument document = courseDocumentMapper.findByIdAndUserId(
                documentId,
                userId
        );

        if (document == null) {
            throw new BusinessException(
                    "DOCUMENT_NOT_FOUND",
                    "Document not found."
            );
        }

        Long courseId = document.getCourseId();

        QuizGenerateOptions options = normalizeOptions(request);

        List<RetrievedChunk> chunks = retrievalService.retrieveDocumentChunks(
                userId,
                courseId,
                documentId,
                options.topK(),
                options.retrievalQuery()
        );

        QuizResult result = generateQuizFromChunks(
                SCOPE_DOCUMENT,
                options.difficulty(),
                options.mcqCount(),
                options.shortAnswerCount(),
                chunks
        );

        QuizDraftValue draftValue = new QuizDraftValue(
                userId,
                courseId,
                documentId,
                SCOPE_DOCUMENT,
                options.mcqCount(),
                options.shortAnswerCount(),
                options.difficulty(),
                result
        );

        Map<String, Object> params = new LinkedHashMap<>();
        params.put("documentId", documentId);
        params.put("topK", options.topK());
        params.put("retrievalQuery", options.retrievalQuery());
        params.put("mcqCount", options.mcqCount());
        params.put("shortAnswerCount", options.shortAnswerCount());
        params.put("difficulty", options.difficulty());

        String draftKey = draftCacheService.buildQuizDraftKey(
                userId,
                courseId,
                SCOPE_DOCUMENT,
                params
        );

        draftCacheService.saveDraft(
                draftKey,
                draftValue,
                Duration.ofDays(7)
        );

        return toGenerateResponse(
                draftKey,
                SCOPE_DOCUMENT,
                result
        );
    }

    @Transactional
    public QuizSaveResponse saveQuiz(
            Long userId,
            SaveDraftRequest request
    ) {
        if (request == null ||
                request.getDraftKey() == null ||
                request.getDraftKey().isBlank()) {
            throw new BusinessException(
                    "INVALID_DRAFT_KEY",
                    "Draft key is required."
            );
        }

        String draftKey = request.getDraftKey();

        draftCacheService.validateDraftOwner(
                draftKey,
                userId
        );

        QuizDraftValue draft = draftCacheService.getDraft(
                draftKey,
                QuizDraftValue.class
        );

        if (draft == null) {
            throw new BusinessException(
                    "DRAFT_NOT_FOUND",
                    "Quiz draft not found."
            );
        }

        validateQuizDraft(
                userId,
                draft
        );

        QuizResult result = draft.getResult();

        quizOutputValidator.validate(
                result,
                draft.getMcqCount(),
                draft.getShortAnswerCount()
        );

        Quiz quiz = new Quiz();
        quiz.setUserId(userId);
        quiz.setCourseId(draft.getCourseId());
        quiz.setDocumentId(draft.getDocumentId());
        quiz.setTitle(result.getTitle());
        quiz.setDifficulty(result.getDifficulty());
        quiz.setSourceScope(draft.getSourceScope());
        quiz.setQuestionCount(result.getQuestions().size());

        quizMapper.insert(quiz);

        for (QuizQuestionResult item : result.getQuestions()) {
            QuizQuestion question = new QuizQuestion();
            question.setQuizId(quiz.getId());
            question.setQuestionType(item.getQuestionType());
            question.setQuestionText(item.getQuestionText());
            question.setOptionsJson(toJson(item.getOptions()));
            question.setCorrectAnswer(item.getCorrectAnswer());
            question.setExplanation(item.getExplanation());
            question.setDifficulty(item.getDifficulty());
            question.setTopic(item.getTopic());
            question.setSourceChunkId(item.getSourceChunkId());

            quizQuestionMapper.insert(question);
        }

        learningHistoryMapper.insertLearningHistory(
                userId,
                quiz.getCourseId(),
                "QUIZ",
                "QUIZ",
                quiz.getId(),
                quiz.getTitle()
        );

        draftCacheService.deleteDraft(draftKey);

        return new QuizSaveResponse(quiz.getId());
    }

    public List<SavedQuizResponse> getCourseQuizzes(
            Long userId,
            Long courseId
    ) {
        validateCourseAccess(
                userId,
                courseId
        );

        return quizMapper.findByUserIdAndCourseId(
                        userId,
                        courseId
                )
                .stream()
                .map(this::toSavedQuizResponse)
                .toList();
    }

    public QuizDetailResponse getQuizDetail(
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
                    "Quiz not found."
            );
        }

        List<SavedQuizQuestionResponse> questions = quizQuestionMapper.findByQuizId(
                        quizId
                )
                .stream()
                .map(this::toSavedQuizQuestionResponse)
                .toList();

        QuizDetailResponse response = new QuizDetailResponse();
        response.setId(quiz.getId());
        response.setUserId(quiz.getUserId());
        response.setCourseId(quiz.getCourseId());
        response.setDocumentId(quiz.getDocumentId());
        response.setTitle(quiz.getTitle());
        response.setDifficulty(quiz.getDifficulty());
        response.setSourceScope(quiz.getSourceScope());
        response.setQuestionCount(quiz.getQuestionCount());
        response.setCreatedAt(quiz.getCreatedAt());
        response.setQuestions(questions);

        return response;
    }

    @Transactional
    public void deleteQuiz(
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
                    "Quiz not found."
            );
        }

        quizQuestionMapper.deleteByQuizId(quizId);

        quizMapper.deleteByIdAndUserId(
                quizId,
                userId
        );
    }

    private QuizResult generateQuizFromChunks(
            String sourceScope,
            String difficulty,
            int mcqCount,
            int shortAnswerCount,
            List<RetrievedChunk> chunks
    ) {
        String prompt = quizPromptBuilder.buildQuizPrompt(
                sourceScope,
                difficulty,
                mcqCount,
                shortAnswerCount,
                chunks
        );

        QuizResult result = callLlmForQuiz(prompt);

        ensureQuizDefaults(
                result,
                difficulty
        );

        quizOutputValidator.validate(
                result,
                mcqCount,
                shortAnswerCount
        );

        return result;
    }

    private QuizResult callLlmForQuiz(String prompt) {
        System.out.println("[QuizService] Start calling LLM for quiz.");
        System.out.println("[QuizService] prompt length = " + prompt.length());

        try {
            System.out.println("[QuizService] About to call LLM.");

            String raw = chatClientBuilder
                    .build()
                    .prompt()
                    .user(prompt)
                    .call()
                    .content();

            System.out.println("[QuizService] LLM returned raw response.");
            System.out.println("[QuizService] raw length = " + (raw == null ? 0 : raw.length()));
            System.out.println("[QuizService] raw preview = " + (
                    raw == null ? "null" : raw.substring(0, Math.min(raw.length(), 500))
            ));
            System.out.println("[QuizService] raw tail = " + (
                    raw == null ? "null" : raw.substring(Math.max(0, raw.length() - 500))
            ));

            if (raw == null || raw.isBlank()) {
                throw new BusinessException(
                        "AI_GENERATION_FAILED",
                        "LLM returned empty quiz."
                );
            }

            String json = extractJson(raw);

            QuizResult result = objectMapper.readValue(
                    json,
                    QuizResult.class
            );

            System.out.println("[QuizService] Quiz JSON parsed.");

            return result;

        } catch (BusinessException e) {
            throw e;

        } catch (Exception e) {
            System.out.println("[QuizService] Failed to generate quiz.");
            System.out.println("[QuizService] exception class = " + e.getClass().getName());
            System.out.println("[QuizService] error message = " + e.getMessage());

            Throwable cause = e.getCause();
            int level = 1;

            while (cause != null && level <= 5) {
                System.out.println("[QuizService] cause " + level + " class = "
                        + cause.getClass().getName());
                System.out.println("[QuizService] cause " + level + " message = "
                        + cause.getMessage());

                cause = cause.getCause();
                level++;
            }

            throw new BusinessException(
                    "AI_GENERATION_FAILED",
                    "Failed to generate quiz. Please try again."
            );
        }
    }

    private void ensureQuizDefaults(
            QuizResult result,
            String difficulty
    ) {
        if (result == null) {
            return;
        }

        if (result.getDifficulty() == null || result.getDifficulty().isBlank()) {
            result.setDifficulty(difficulty);
        }

        if (result.getTitle() == null || result.getTitle().isBlank()) {
            result.setTitle("Generated Quiz");
        }

        if (result.getQuestions() == null) {
            result.setQuestions(new ArrayList<>());
        }

        for (QuizQuestionResult question : result.getQuestions()) {
            if (question.getDifficulty() == null || question.getDifficulty().isBlank()) {
                question.setDifficulty(difficulty);
            }

            if (question.getTopic() == null || question.getTopic().isBlank()) {
                question.setTopic(result.getTitle());
            }

            if (question.getOptions() == null) {
                question.setOptions(new ArrayList<>());
            }
        }
    }

    private void validateQuizDraft(
            Long userId,
            QuizDraftValue draft
    ) {
        if (!Objects.equals(userId, draft.getUserId())) {
            throw new BusinessException(
                    "FORBIDDEN_DRAFT",
                    "You cannot save this quiz draft."
            );
        }

        validateCourseAccess(
                userId,
                draft.getCourseId()
        );

        if (draft.getDocumentId() != null) {
            CourseDocument document = courseDocumentMapper.findByIdAndUserId(
                    draft.getDocumentId(),
                    userId
            );

            if (document == null) {
                throw new BusinessException(
                        "DOCUMENT_NOT_FOUND",
                        "Document not found."
                );
            }

            if (!Objects.equals(document.getCourseId(), draft.getCourseId())) {
                throw new BusinessException(
                        "DOCUMENT_ACCESS_DENIED",
                        "Document does not belong to this course."
                );
            }
        }
    }

    private void validateCourseAccess(
            Long userId,
            Long courseId
    ) {
        Course course = courseMapper.findByIdAndUserId(
                courseId,
                userId
        );

        if (course == null) {
            throw new BusinessException(
                    "COURSE_ACCESS_DENIED",
                    "Course not found or access denied."
            );
        }
    }

    private QuizGenerateOptions normalizeOptions(QuizGenerateRequest request) {
        int topK = request == null || request.getTopK() == null
                ? 1
                : Math.max(1, Math.min(request.getTopK(), 5));

        String retrievalQuery = request == null
                ? null
                : request.getRetrievalQuery();

        int mcqCount = request == null || request.getMcqCount() == null
                ? 2
                : Math.max(0, Math.min(request.getMcqCount(), 5));

        int shortAnswerCount = request == null || request.getShortAnswerCount() == null
                ? 1
                : Math.max(0, Math.min(request.getShortAnswerCount(), 5));

        if (mcqCount + shortAnswerCount <= 0) {
            mcqCount = 2;
            shortAnswerCount = 1;
        }

        String difficulty = request == null || request.getDifficulty() == null
                ? "MEDIUM"
                : request.getDifficulty().trim().toUpperCase();

        if (!Set.of("EASY", "MEDIUM", "HARD").contains(difficulty)) {
            difficulty = "MEDIUM";
        }

        return new QuizGenerateOptions(
                topK,
                retrievalQuery,
                mcqCount,
                shortAnswerCount,
                difficulty
        );
    }

    private String extractJson(String raw) {
        String cleaned = raw.trim();

        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7).trim();
        }

        if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3).trim();
        }

        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3).trim();
        }

        int start = cleaned.indexOf("{");
        int end = cleaned.lastIndexOf("}");

        if (start < 0 || end < 0 || end <= start) {
            throw new BusinessException(
                    "AI_OUTPUT_INVALID",
                    "LLM did not return valid JSON."
            );
        }

        return cleaned.substring(start, end + 1);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new BusinessException(
                    "JSON_SERIALIZATION_FAILED",
                    "Failed to serialize quiz options."
            );
        }
    }

    private QuizGenerateResponse toGenerateResponse(
            String draftKey,
            String sourceScope,
            QuizResult result
    ) {
        return new QuizGenerateResponse(
                draftKey,
                result.getTitle(),
                result.getDifficulty(),
                sourceScope,
                result.getQuestions() == null ? 0 : result.getQuestions().size(),
                result.getQuestions()
        );
    }

    private SavedQuizResponse toSavedQuizResponse(Quiz quiz) {
        return new SavedQuizResponse(
                quiz.getId(),
                quiz.getUserId(),
                quiz.getCourseId(),
                quiz.getDocumentId(),
                quiz.getTitle(),
                quiz.getDifficulty(),
                quiz.getSourceScope(),
                quiz.getQuestionCount(),
                quiz.getCreatedAt()
        );
    }

    private SavedQuizQuestionResponse toSavedQuizQuestionResponse(
            QuizQuestion question
    ) {
        return new SavedQuizQuestionResponse(
                question.getId(),
                question.getQuizId(),
                question.getQuestionType(),
                question.getQuestionText(),
                question.getOptionsJson(),
                question.getCorrectAnswer(),
                question.getExplanation(),
                question.getDifficulty(),
                question.getTopic(),
                question.getSourceChunkId(),
                question.getCreatedAt()
        );
    }

    private record QuizGenerateOptions(
            int topK,
            String retrievalQuery,
            int mcqCount,
            int shortAnswerCount,
            String difficulty
    ) {
    }
}