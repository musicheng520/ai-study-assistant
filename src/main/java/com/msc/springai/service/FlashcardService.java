package com.msc.springai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.msc.springai.constant.AiWorkflowTypes;
import com.msc.springai.dto.learning.draft.FlashcardDraftValue;
import com.msc.springai.dto.learning.request.FlashcardGenerateRequest;
import com.msc.springai.dto.learning.request.SaveDraftRequest;
import com.msc.springai.dto.learning.request.WrongTopicFlashcardGenerateRequest;
import com.msc.springai.dto.learning.response.FlashcardGenerateResponse;
import com.msc.springai.dto.learning.response.FlashcardSaveResponse;
import com.msc.springai.dto.learning.response.SavedFlashcardResponse;
import com.msc.springai.dto.learning.response.WeakTopicFlashcardGenerateResponse;
import com.msc.springai.dto.learning.response.WeakTopicResponse;
import com.msc.springai.dto.learning.result.FlashcardItemResult;
import com.msc.springai.dto.learning.result.FlashcardResult;
import com.msc.springai.dto.learning.retrieval.RetrievedChunk;
import com.msc.springai.entity.Course;
import com.msc.springai.entity.CourseDocument;
import com.msc.springai.entity.Flashcard;
import com.msc.springai.exception.BusinessException;
import com.msc.springai.mapper.CourseDocumentMapper;
import com.msc.springai.mapper.CourseMapper;
import com.msc.springai.mapper.FlashcardMapper;
import com.msc.springai.mapper.LearningHistoryMapper;
import com.msc.springai.mapper.WrongAnswerMapper;
import com.msc.springai.service.observability.AiChatResponseUtil;
import com.msc.springai.service.observability.AiRequestLogContext;
import com.msc.springai.service.observability.AiRequestLogService;
import com.msc.springai.service.prompt.FlashcardPromptBuilder;
import com.msc.springai.service.validator.FlashcardOutputValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class FlashcardService {

    private static final String SCOPE_COURSE = "COURSE";
    private static final String SCOPE_DOCUMENT = "DOCUMENT";
    private static final String SCOPE_WEAK_TOPIC = "WEAK_TOPIC";

    private static final String SOURCE_TYPE_QUIZ_WRONG_TOPIC = "QUIZ_WRONG_TOPIC";

    private final RetrievalService retrievalService;
    private final DraftCacheService draftCacheService;
    private final AiRequestLogService aiRequestLogService;
    private final FlashcardOutputValidator flashcardOutputValidator;
    private final FlashcardPromptBuilder flashcardPromptBuilder;

    private final FlashcardMapper flashcardMapper;
    private final CourseMapper courseMapper;
    private final CourseDocumentMapper courseDocumentMapper;
    private final LearningHistoryMapper learningHistoryMapper;
    private final WrongAnswerMapper wrongAnswerMapper;

    private final ObjectMapper objectMapper;
    private final ChatClient.Builder chatClientBuilder;

    public FlashcardGenerateResponse generateCourseFlashcards(
            Long userId,
            Long courseId,
            FlashcardGenerateRequest request
    ) {
        validateCourseAccess(
                userId,
                courseId
        );

        FlashcardGenerateOptions options =
                normalizeOptions(request);

        List<RetrievedChunk> chunks =
                retrievalService.retrieveCourseChunks(
                        userId,
                        courseId,
                        options.topK(),
                        options.retrievalQuery()
                );

        FlashcardResult result =
                generateFlashcardsFromChunks(
                        userId,
                        courseId,
                        SCOPE_COURSE,
                        options.count(),
                        options.difficulty(),
                        chunks
                );

        FlashcardDraftValue draftValue =
                new FlashcardDraftValue(
                        userId,
                        courseId,
                        null,
                        SCOPE_COURSE,
                        options.count(),
                        options.difficulty(),
                        result
                );

        Map<String, Object> params =
                new LinkedHashMap<>();

        params.put("documentId", null);
        params.put("topK", options.topK());
        params.put(
                "retrievalQuery",
                options.retrievalQuery()
        );
        params.put("count", options.count());
        params.put(
                "difficulty",
                options.difficulty()
        );

        String draftKey =
                draftCacheService.buildFlashcardDraftKey(
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
                options.difficulty(),
                result
        );
    }

    public FlashcardGenerateResponse generateDocumentFlashcards(
            Long userId,
            Long documentId,
            FlashcardGenerateRequest request
    ) {
        CourseDocument document =
                courseDocumentMapper.findByIdAndUserId(
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

        FlashcardGenerateOptions options =
                normalizeOptions(request);

        List<RetrievedChunk> chunks =
                retrievalService.retrieveDocumentChunks(
                        userId,
                        courseId,
                        documentId,
                        options.topK(),
                        options.retrievalQuery()
                );

        FlashcardResult result =
                generateFlashcardsFromChunks(
                        userId,
                        courseId,
                        SCOPE_DOCUMENT,
                        options.count(),
                        options.difficulty(),
                        chunks
                );

        FlashcardDraftValue draftValue =
                new FlashcardDraftValue(
                        userId,
                        courseId,
                        documentId,
                        SCOPE_DOCUMENT,
                        options.count(),
                        options.difficulty(),
                        result
                );

        Map<String, Object> params =
                new LinkedHashMap<>();

        params.put("documentId", documentId);
        params.put("topK", options.topK());
        params.put(
                "retrievalQuery",
                options.retrievalQuery()
        );
        params.put("count", options.count());
        params.put(
                "difficulty",
                options.difficulty()
        );

        String draftKey =
                draftCacheService.buildFlashcardDraftKey(
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
                options.difficulty(),
                result
        );
    }

    public WeakTopicFlashcardGenerateResponse
    generateFlashcardsFromWrongTopics(
            Long userId,
            Long courseId,
            WrongTopicFlashcardGenerateRequest request
    ) {
        validateCourseAccess(
                userId,
                courseId
        );

        WrongTopicFlashcardOptions options =
                normalizeWrongTopicOptions(request);

        List<WeakTopicResponse> weakTopics =
                wrongAnswerMapper.findWeakTopics(
                        userId,
                        courseId
                );

        List<String> topics = weakTopics.stream()
                .filter(topic ->
                        topic.getUnresolvedCount() != null
                )
                .filter(topic ->
                        topic.getUnresolvedCount() > 0
                )
                .limit(options.topicLimit())
                .map(WeakTopicResponse::getTopic)
                .filter(topic ->
                        topic != null
                                && !topic.isBlank()
                )
                .map(String::trim)
                .toList();

        if (topics.isEmpty()) {
            throw new BusinessException(
                    "NO_UNRESOLVED_WRONG_TOPICS",
                    "No unresolved wrong topics found for this course."
            );
        }

        String retrievalQuery =
                String.join("; ", topics);

        List<RetrievedChunk> chunks =
                retrievalService.retrieveCourseChunks(
                        userId,
                        courseId,
                        options.topK(),
                        retrievalQuery
                );

        int expectedCardCount =
                topics.size()
                        * options.cardsPerTopic();

        String prompt =
                flashcardPromptBuilder
                        .buildWeakTopicFlashcardPrompt(
                                topics,
                                options.cardsPerTopic(),
                                options.difficulty(),
                                chunks
                        );

        AiRequestLogContext logContext =
                aiRequestLogService.start(
                        userId,
                        courseId,
                        AiWorkflowTypes.FLASHCARD
                );

        aiRequestLogService.setRetrievedChunkCount(
                logContext,
                chunks == null
                        ? 0
                        : chunks.size()
        );

        try {
            FlashcardResult result =
                    callLlmForFlashcards(
                            prompt,
                            logContext
                    );

            ensureFlashcardDefaults(
                    result,
                    options.difficulty()
            );

            normalizeWeakTopicCards(
                    result,
                    topics,
                    options.difficulty()
            );

            flashcardOutputValidator.validate(
                    result,
                    expectedCardCount
            );

            FlashcardDraftValue draftValue =
                    new FlashcardDraftValue(
                            userId,
                            courseId,
                            null,
                            SCOPE_WEAK_TOPIC,
                            expectedCardCount,
                            options.difficulty(),
                            result
                    );

            Map<String, Object> params =
                    new LinkedHashMap<>();

            params.put(
                    "topicLimit",
                    options.topicLimit()
            );

            params.put(
                    "cardsPerTopic",
                    options.cardsPerTopic()
            );

            params.put(
                    "difficulty",
                    options.difficulty()
            );

            params.put(
                    "topK",
                    options.topK()
            );

            params.put(
                    "topics",
                    topics
            );

            String draftKey =
                    draftCacheService
                            .buildFlashcardDraftKey(
                                    userId,
                                    courseId,
                                    SCOPE_WEAK_TOPIC,
                                    params
                            );

            draftCacheService.saveDraft(
                    draftKey,
                    draftValue,
                    Duration.ofDays(7)
            );

            /*
             * LLM、JSON 解析、Validator 和
             * Redis Draft 都成功后再记录成功。
             */
            aiRequestLogService.completeSuccess(
                    logContext
            );

            return new WeakTopicFlashcardGenerateResponse(
                    draftKey,
                    SOURCE_TYPE_QUIZ_WRONG_TOPIC,
                    topics,
                    result.getCards()
            );

        } catch (BusinessException exception) {
            aiRequestLogService.completeFailure(
                    logContext,
                    exception
            );

            throw exception;

        } catch (Exception exception) {
            aiRequestLogService.completeFailure(
                    logContext,
                    exception
            );

            throw new BusinessException(
                    "AI_GENERATION_FAILED",
                    "Failed to generate weak-topic flashcards."
            );
        }
    }

    @Transactional
    public FlashcardSaveResponse saveFlashcards(
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

        FlashcardDraftValue draft = draftCacheService.getDraft(
                draftKey,
                FlashcardDraftValue.class
        );

        if (draft == null) {
            throw new BusinessException(
                    "DRAFT_NOT_FOUND",
                    "Flashcard draft not found."
            );
        }

        validateFlashcardDraft(
                userId,
                draft
        );

        FlashcardResult result = draft.getResult();

        ensureFlashcardDefaults(
                result,
                draft.getDifficulty()
        );

        flashcardOutputValidator.validate(
                result,
                draft.getCount()
        );

        List<Long> flashcardIds = new ArrayList<>();

        for (FlashcardItemResult item : result.getCards()) {
            Flashcard flashcard = new Flashcard();
            flashcard.setUserId(userId);
            flashcard.setCourseId(draft.getCourseId());
            flashcard.setDocumentId(draft.getDocumentId());
            flashcard.setFront(item.getFront());
            flashcard.setBack(item.getBack());
            flashcard.setTopic(item.getTopic());
            flashcard.setDifficulty(item.getDifficulty());
            flashcard.setSourceType(toFlashcardSourceType(draft.getSourceScope()));
            flashcard.setSourceChunkId(item.getSourceChunkId());

            flashcardMapper.insert(flashcard);
            flashcardIds.add(flashcard.getId());
        }

        Long firstFlashcardId = flashcardIds.isEmpty()
                ? null
                : flashcardIds.get(0);

        learningHistoryMapper.insertLearningHistory(
                userId,
                draft.getCourseId(),
                "FLASHCARD",
                "FLASHCARD",
                firstFlashcardId,
                result.getTitle()
        );

        draftCacheService.deleteDraft(draftKey);

        return new FlashcardSaveResponse(
                flashcardIds.size(),
                flashcardIds
        );
    }

    public List<SavedFlashcardResponse> getCourseFlashcards(
            Long userId,
            Long courseId
    ) {
        validateCourseAccess(
                userId,
                courseId
        );

        return flashcardMapper.findByUserIdAndCourseId(
                        userId,
                        courseId
                )
                .stream()
                .map(this::toSavedFlashcardResponse)
                .toList();
    }

    @Transactional
    public void deleteFlashcard(
            Long userId,
            Long flashcardId
    ) {
        Flashcard flashcard = flashcardMapper.findByIdAndUserId(
                flashcardId,
                userId
        );

        if (flashcard == null) {
            throw new BusinessException(
                    "FLASHCARD_NOT_FOUND",
                    "Flashcard not found."
            );
        }

        flashcardMapper.deleteByIdAndUserId(
                flashcardId,
                userId
        );
    }

    private FlashcardResult generateFlashcardsFromChunks(
            Long userId,
            Long courseId,
            String sourceScope,
            int count,
            String difficulty,
            List<RetrievedChunk> chunks
    ) {
        String prompt =
                flashcardPromptBuilder
                        .buildFlashcardPrompt(
                                sourceScope,
                                count,
                                difficulty,
                                chunks
                        );

        AiRequestLogContext logContext =
                aiRequestLogService.start(
                        userId,
                        courseId,
                        AiWorkflowTypes.FLASHCARD
                );

        aiRequestLogService.setRetrievedChunkCount(
                logContext,
                chunks == null
                        ? 0
                        : chunks.size()
        );

        try {
            FlashcardResult result =
                    callLlmForFlashcards(
                            prompt,
                            logContext
                    );

            ensureFlashcardDefaults(
                    result,
                    difficulty
            );

            flashcardOutputValidator.validate(
                    result,
                    count
            );

            aiRequestLogService.completeSuccess(
                    logContext
            );

            return result;

        } catch (BusinessException exception) {
            aiRequestLogService.completeFailure(
                    logContext,
                    exception
            );

            throw exception;

        } catch (Exception exception) {
            aiRequestLogService.completeFailure(
                    logContext,
                    exception
            );

            throw new BusinessException(
                    "AI_GENERATION_FAILED",
                    "Failed to generate flashcards. Please try again."
            );
        }
    }

    private FlashcardResult callLlmForFlashcards(
            String prompt,
            AiRequestLogContext logContext
    ) {
        System.out.println(
                "[FlashcardService] Start calling LLM for flashcards."
        );

        System.out.println(
                "[FlashcardService] prompt length = "
                        + (prompt == null
                        ? 0
                        : prompt.length())
        );

        try {
            System.out.println(
                    "[FlashcardService] About to call LLM."
            );

            ChatResponse chatResponse =
                    chatClientBuilder
                            .build()
                            .prompt()
                            .user(prompt)
                            .call()
                            .chatResponse();

            /*
             * 从完整 ChatResponse 中提取：
             *
             * model name
             * prompt tokens
             * completion tokens
             * total tokens
             */
            aiRequestLogService.captureResponseMetadata(
                    logContext,
                    chatResponse
            );

            String raw =
                    AiChatResponseUtil.extractText(
                            chatResponse
                    );

            System.out.println(
                    "[FlashcardService] LLM returned raw response."
            );

            System.out.println(
                    "[FlashcardService] raw length = "
                            + (raw == null
                            ? 0
                            : raw.length())
            );

            System.out.println(
                    "[FlashcardService] raw preview = "
                            + (
                            raw == null
                                    ? "null"
                                    : raw.substring(
                                    0,
                                    Math.min(
                                            raw.length(),
                                            500
                                    )
                            )
                    )
            );

            System.out.println(
                    "[FlashcardService] raw tail = "
                            + (
                            raw == null
                                    ? "null"
                                    : raw.substring(
                                    Math.max(
                                            0,
                                            raw.length() - 500
                                    )
                            )
                    )
            );

            if (raw == null || raw.isBlank()) {
                throw new BusinessException(
                        "AI_GENERATION_FAILED",
                        "LLM returned empty flashcards."
                );
            }

            String json =
                    extractJson(raw);

            FlashcardResult result =
                    objectMapper.readValue(
                            json,
                            FlashcardResult.class
                    );

            System.out.println(
                    "[FlashcardService] Flashcard JSON parsed."
            );

            return result;

        } catch (BusinessException exception) {
            throw exception;

        } catch (Exception exception) {
            System.out.println(
                    "[FlashcardService] Failed to generate flashcards."
            );

            System.out.println(
                    "[FlashcardService] exception class = "
                            + exception
                            .getClass()
                            .getName()
            );

            System.out.println(
                    "[FlashcardService] error message = "
                            + exception.getMessage()
            );

            Throwable cause =
                    exception.getCause();

            int level = 1;

            while (cause != null && level <= 5) {
                System.out.println(
                        "[FlashcardService] cause "
                                + level
                                + " class = "
                                + cause
                                .getClass()
                                .getName()
                );

                System.out.println(
                        "[FlashcardService] cause "
                                + level
                                + " message = "
                                + cause.getMessage()
                );

                cause = cause.getCause();
                level++;
            }

            throw new BusinessException(
                    "AI_GENERATION_FAILED",
                    "Failed to generate flashcards. Please try again."
            );
        }
    }

    private void ensureFlashcardDefaults(
            FlashcardResult result,
            String difficulty
    ) {
        if (result == null) {
            return;
        }

        if (result.getTitle() == null || result.getTitle().isBlank()) {
            result.setTitle("Generated Flashcards");
        }

        if (result.getCards() == null) {
            result.setCards(new ArrayList<>());
        }

        for (FlashcardItemResult card : result.getCards()) {
            if (card.getDifficulty() == null || card.getDifficulty().isBlank()) {
                card.setDifficulty(difficulty);
            }

            if (card.getTopic() == null || card.getTopic().isBlank()) {
                card.setTopic(result.getTitle());
            }
        }
    }

    private void normalizeWeakTopicCards(
            FlashcardResult result,
            List<String> topics,
            String difficulty
    ) {
        if (result == null || result.getCards() == null || result.getCards().isEmpty()) {
            return;
        }

        for (int i = 0; i < result.getCards().size(); i++) {
            FlashcardItemResult card = result.getCards().get(i);

            if (card.getDifficulty() == null || card.getDifficulty().isBlank()) {
                card.setDifficulty(difficulty);
            }

            if (card.getTopic() == null || card.getTopic().isBlank()) {
                card.setTopic(topics.get(i % topics.size()));
                continue;
            }

            String canonicalTopic = findCanonicalTopic(
                    card.getTopic(),
                    topics
            );

            if (canonicalTopic == null) {
                card.setTopic(topics.get(i % topics.size()));
            } else {
                card.setTopic(canonicalTopic);
            }
        }
    }

    private String findCanonicalTopic(
            String topic,
            List<String> allowedTopics
    ) {
        if (topic == null || allowedTopics == null || allowedTopics.isEmpty()) {
            return null;
        }

        for (String allowedTopic : allowedTopics) {
            if (allowedTopic.equalsIgnoreCase(topic.trim())) {
                return allowedTopic;
            }
        }

        return null;
    }

    private void validateFlashcardDraft(
            Long userId,
            FlashcardDraftValue draft
    ) {
        if (!Objects.equals(userId, draft.getUserId())) {
            throw new BusinessException(
                    "FORBIDDEN_DRAFT",
                    "You cannot save this flashcard draft."
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

    private FlashcardGenerateOptions normalizeOptions(
            FlashcardGenerateRequest request
    ) {
        int topK = request == null || request.getTopK() == null
                ? 1
                : Math.max(1, Math.min(request.getTopK(), 5));

        String retrievalQuery = request == null
                ? null
                : request.getRetrievalQuery();

        int count = request == null || request.getCount() == null
                ? 4
                : Math.max(1, Math.min(request.getCount(), 8));

        String difficulty = request == null || request.getDifficulty() == null
                ? "MEDIUM"
                : request.getDifficulty().trim().toUpperCase();

        if (!Set.of("EASY", "MEDIUM", "HARD").contains(difficulty)) {
            difficulty = "MEDIUM";
        }

        return new FlashcardGenerateOptions(
                topK,
                retrievalQuery,
                count,
                difficulty
        );
    }

    private WrongTopicFlashcardOptions normalizeWrongTopicOptions(
            WrongTopicFlashcardGenerateRequest request
    ) {
        int topicLimit = request == null || request.getTopicLimit() == null
                ? 3
                : Math.max(1, Math.min(request.getTopicLimit(), 5));

        int cardsPerTopic = request == null || request.getCardsPerTopic() == null
                ? 3
                : Math.max(1, Math.min(request.getCardsPerTopic(), 5));

        String difficulty = request == null || request.getDifficulty() == null
                ? "MEDIUM"
                : request.getDifficulty().trim().toUpperCase();

        if (!Set.of("EASY", "MEDIUM", "HARD").contains(difficulty)) {
            difficulty = "MEDIUM";
        }

        int defaultTopK = topicLimit * 3;

        int topK = request == null || request.getTopK() == null
                ? defaultTopK
                : Math.max(1, Math.min(request.getTopK(), 10));

        return new WrongTopicFlashcardOptions(
                topicLimit,
                cardsPerTopic,
                difficulty,
                topK
        );
    }

    private String toFlashcardSourceType(String sourceScope) {
        if (sourceScope == null || sourceScope.isBlank()) {
            return "MANUAL";
        }

        if (SCOPE_WEAK_TOPIC.equalsIgnoreCase(sourceScope)) {
            return SOURCE_TYPE_QUIZ_WRONG_TOPIC;
        }

        return sourceScope;
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

    private FlashcardGenerateResponse toGenerateResponse(
            String draftKey,
            String sourceScope,
            String difficulty,
            FlashcardResult result
    ) {
        return new FlashcardGenerateResponse(
                draftKey,
                result.getTitle(),
                sourceScope,
                result.getCards() == null ? 0 : result.getCards().size(),
                difficulty,
                result.getCards()
        );
    }

    private SavedFlashcardResponse toSavedFlashcardResponse(
            Flashcard flashcard
    ) {
        return new SavedFlashcardResponse(
                flashcard.getId(),
                flashcard.getUserId(),
                flashcard.getCourseId(),
                flashcard.getDocumentId(),
                flashcard.getFront(),
                flashcard.getBack(),
                flashcard.getTopic(),
                flashcard.getDifficulty(),
                flashcard.getSourceType(),
                flashcard.getSourceChunkId(),
                flashcard.getCreatedAt()
        );
    }

    private record FlashcardGenerateOptions(
            int topK,
            String retrievalQuery,
            int count,
            String difficulty
    ) {
    }

    private record WrongTopicFlashcardOptions(
            int topicLimit,
            int cardsPerTopic,
            String difficulty,
            int topK
    ) {
    }
}