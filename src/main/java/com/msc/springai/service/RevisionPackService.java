package com.msc.springai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.msc.springai.constant.AiWorkflowTypes;
import com.msc.springai.dto.workflow.revision.GenerateRevisionPackRequest;
import com.msc.springai.dto.workflow.revision.RevisionPackResponse;
import com.msc.springai.dto.workflow.revision.RevisionPackResult;
import com.msc.springai.dto.workflow.revision.RevisionRelatedDocumentResult;
import com.msc.springai.dto.workflow.revision.RevisionWeakTopicResult;
import com.msc.springai.dto.workflow.tool.StudyToolDtos.CourseDocumentToolResult;
import com.msc.springai.dto.workflow.tool.StudyToolDtos.CourseProgressToolResult;
import com.msc.springai.dto.workflow.tool.StudyToolDtos.DocumentSearchToolResult;
import com.msc.springai.dto.workflow.tool.StudyToolDtos.LearningHistoryToolResult;
import com.msc.springai.dto.workflow.tool.StudyToolDtos.NoteToolResult;
import com.msc.springai.dto.workflow.tool.StudyToolDtos.WrongAnswerItem;
import com.msc.springai.dto.workflow.tool.StudyToolDtos.WrongAnswerTopicSummary;
import com.msc.springai.dto.workflow.tool.StudyToolDtos.WrongAnswersToolResult;
import com.msc.springai.entity.RevisionPack;
import com.msc.springai.exception.BusinessException;
import com.msc.springai.mapper.RevisionPackMapper;
import com.msc.springai.service.observability.AiChatResponseUtil;
import com.msc.springai.service.observability.AiRequestLogContext;
import com.msc.springai.service.observability.AiRequestLogService;
import com.msc.springai.service.validator.RevisionPackValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RevisionPackService {

    private final RevisionPackMapper revisionPackMapper;
    private final StudyAgentToolService studyAgentToolService;
    private final StudyDocumentSearchToolService studyDocumentSearchToolService;
    private final RevisionPackValidator revisionPackValidator;
    private final ObjectMapper objectMapper;
    private final ChatClient.Builder chatClientBuilder;
    private final AiRequestLogService aiRequestLogService;

    @Transactional
    public RevisionPackResponse generateRevisionPack(
            Long currentUserId,
            Long courseId,
            GenerateRevisionPackRequest request
    ) {
        System.out.println("[RevisionPackService] ===== generateRevisionPack START =====");
        System.out.println("[RevisionPackService] currentUserId = " + currentUserId);
        System.out.println("[RevisionPackService] courseId = " + courseId);

        validateCurrentUser(currentUserId);
        validateCourseId(courseId);
        ensureCourseAccess(currentUserId, courseId);

        AiRequestLogContext logContext =
                aiRequestLogService.start(
                        currentUserId,
                        courseId,
                        AiWorkflowTypes.REVISION_PACK
                );

        try {
            int maxWeakTopics =
                    normalizeMaxWeakTopics(
                            request == null
                                    ? null
                                    : request.getMaxWeakTopics()
                    );

            int maxRelatedChunks =
                    normalizeMaxRelatedChunks(
                            request == null
                                    ? null
                                    : request.getMaxRelatedChunks()
                    );

            System.out.println("[RevisionPackService] maxWeakTopics = " + maxWeakTopics);
            System.out.println("[RevisionPackService] maxRelatedChunks = " + maxRelatedChunks);

            System.out.println("[RevisionPackService] Step 1: load wrong answers");
            WrongAnswersToolResult wrongAnswers =
                    studyAgentToolService.getWrongAnswersTool(
                            currentUserId,
                            courseId,
                            false,
                            20
                    );
            System.out.println("[RevisionPackService] wrongAnswers loaded");

            System.out.println("[RevisionPackService] Step 2: load progress");
            CourseProgressToolResult progress =
                    studyAgentToolService.getCourseProgressTool(
                            currentUserId,
                            courseId
                    );
            System.out.println("[RevisionPackService] progress loaded");

            System.out.println("[RevisionPackService] Step 3: load learning history");
            List<LearningHistoryToolResult> learningHistory =
                    studyAgentToolService.searchLearningHistoryTool(
                            currentUserId,
                            courseId,
                            10
                    );
            System.out.println("[RevisionPackService] learningHistory size = "
                    + (learningHistory == null ? 0 : learningHistory.size()));

            System.out.println("[RevisionPackService] Step 4: load notes");
            List<NoteToolResult> notes =
                    studyAgentToolService.searchNotesTool(
                            currentUserId,
                            courseId,
                            null,
                            null,
                            10
                    );
            System.out.println("[RevisionPackService] notes size = "
                    + (notes == null ? 0 : notes.size()));

            System.out.println("[RevisionPackService] Step 5: load course documents");
            List<CourseDocumentToolResult> documents =
                    studyAgentToolService.getCourseDocumentsTool(
                            currentUserId,
                            courseId,
                            null
                    );
            System.out.println("[RevisionPackService] documents size = "
                    + (documents == null ? 0 : documents.size()));

            System.out.println("[RevisionPackService] Step 6: extract weak topics");
            List<String> weakTopicNames =
                    extractWeakTopicNames(
                            wrongAnswers,
                            progress,
                            maxWeakTopics
                    );
            System.out.println("[RevisionPackService] weakTopicNames = " + weakTopicNames);

            System.out.println("[RevisionPackService] Step 7: retrieve related chunks");
            List<DocumentSearchToolResult> relatedChunks =
                    retrieveRelatedChunks(
                            currentUserId,
                            courseId,
                            weakTopicNames,
                            maxRelatedChunks
                    );
            System.out.println("[RevisionPackService] relatedChunks size = "
                    + (relatedChunks == null ? 0 : relatedChunks.size()));

            aiRequestLogService.setRetrievedChunkCount(
                    logContext,
                    relatedChunks == null
                            ? 0
                            : relatedChunks.size()
            );

            System.out.println("[RevisionPackService] Step 8: build prompt");
            String prompt =
                    buildPrompt(
                            wrongAnswers,
                            progress,
                            learningHistory,
                            notes,
                            documents,
                            relatedChunks
                    );

            System.out.println("[RevisionPackService] prompt length = " + prompt.length());

            System.out.println("[RevisionPackService] Step 9: call LLM");
            System.out.println("[RevisionPackService] Start LLM revision pack generation.");

            ChatResponse chatResponse =
                    chatClientBuilder
                            .build()
                            .prompt()
                            .user(prompt)
                            .call()
                            .chatResponse();

            aiRequestLogService.captureResponseMetadata(
                    logContext,
                    chatResponse
            );

            String rawContent =
                    AiChatResponseUtil.extractText(
                            chatResponse
                    );

            System.out.println("[RevisionPackService] LLM raw content:");
            System.out.println(rawContent);

            System.out.println("[RevisionPackService] Step 10: parse LLM result");
            RevisionPackResult result =
                    parseResult(rawContent);

            System.out.println("[RevisionPackService] Step 11: validate revision pack");
            revisionPackValidator.validate(result);

            System.out.println("[RevisionPackService] Step 12: convert to entity");
            RevisionPack revisionPack =
                    toEntity(
                            currentUserId,
                            courseId,
                            result
                    );

            System.out.println("[RevisionPackService] Step 13: insert revision_packs");
            revisionPackMapper.insert(revisionPack);
            System.out.println("[RevisionPackService] revisionPack inserted, id = "
                    + revisionPack.getId());

            System.out.println("[RevisionPackService] Step 14: insert learning_history safely");
            insertLearningHistorySafely(
                    currentUserId,
                    courseId,
                    revisionPack.getId()
            );

            RevisionPackResponse response =
                    toResponse(revisionPack);

            aiRequestLogService.completeSuccess(
                    logContext
            );

            System.out.println("[RevisionPackService] ===== generateRevisionPack SUCCESS =====");

            return response;

        } catch (BusinessException exception) {
            aiRequestLogService.completeFailure(
                    logContext,
                    exception
            );

            throw exception;

        } catch (Exception exception) {
            System.out.println("[RevisionPackService] generateRevisionPack failed.");
            System.out.println("[RevisionPackService] Error class = "
                    + exception.getClass().getName());
            System.out.println("[RevisionPackService] Error message = "
                    + exception.getMessage());

            aiRequestLogService.completeFailure(
                    logContext,
                    exception
            );

            throw new BusinessException(
                    "REVISION_PACK_FAILED",
                    "Failed to generate revision pack. Please try again."
            );
        }
    }

    public List<RevisionPackResponse> getCourseRevisionPacks(
            Long currentUserId,
            Long courseId
    ) {
        validateCurrentUser(currentUserId);
        validateCourseId(courseId);
        ensureCourseAccess(currentUserId, courseId);

        return revisionPackMapper
                .findByCourseIdAndUserId(
                        currentUserId,
                        courseId
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public RevisionPackResponse getRevisionPackDetail(
            Long currentUserId,
            Long packId
    ) {
        validateCurrentUser(currentUserId);

        if (packId == null) {
            throw new BusinessException(
                    "INVALID_REVISION_PACK_ID",
                    "Revision pack id is required."
            );
        }

        RevisionPack pack =
                revisionPackMapper.findByIdAndUserId(
                        packId,
                        currentUserId
                );

        if (pack == null) {
            throw new BusinessException(
                    "REVISION_PACK_NOT_FOUND",
                    "Revision pack not found or access denied."
            );
        }

        return toResponse(pack);
    }

    private List<String> extractWeakTopicNames(
            WrongAnswersToolResult wrongAnswers,
            CourseProgressToolResult progress,
            int maxWeakTopics
    ) {
        List<String> topics =
                new ArrayList<>();

        if (wrongAnswers != null
                && wrongAnswers.getWeakTopics() != null) {
            for (WrongAnswerTopicSummary topicSummary :
                    wrongAnswers.getWeakTopics()) {
                addTopicIfValid(
                        topics,
                        topicSummary,
                        maxWeakTopics
                );

                if (topics.size() >= maxWeakTopics) {
                    return topics;
                }
            }
        }

        if (progress != null
                && progress.getWeakTopics() != null) {
            for (WrongAnswerTopicSummary topicSummary :
                    progress.getWeakTopics()) {
                addTopicIfValid(
                        topics,
                        topicSummary,
                        maxWeakTopics
                );

                if (topics.size() >= maxWeakTopics) {
                    return topics;
                }
            }
        }

        return topics;
    }

    private void addTopicIfValid(
            List<String> topics,
            WrongAnswerTopicSummary topicSummary,
            int maxWeakTopics
    ) {
        if (topicSummary == null
                || topicSummary.getTopic() == null
                || topicSummary.getTopic().isBlank()) {
            return;
        }

        String topic =
                topicSummary.getTopic().trim();

        if (!topics.contains(topic)
                && topics.size() < maxWeakTopics) {
            topics.add(topic);
        }
    }

    private List<DocumentSearchToolResult> retrieveRelatedChunks(
            Long userId,
            Long courseId,
            List<String> weakTopicNames,
            int maxRelatedChunks
    ) {
        List<DocumentSearchToolResult> relatedChunks =
                new ArrayList<>();

        if (weakTopicNames == null || weakTopicNames.isEmpty()) {
            System.out.println("[RevisionPackService] No weak topics found. Skip related chunk retrieval.");
            return relatedChunks;
        }

        for (String topic : weakTopicNames) {
            if (topic == null || topic.isBlank()) {
                continue;
            }

            try {
                System.out.println("[RevisionPackService] Retrieve chunks for topic = " + topic);

                List<DocumentSearchToolResult> chunks =
                        studyDocumentSearchToolService.searchDocumentTool(
                                userId,
                                courseId,
                                topic,
                                maxRelatedChunks,
                                null
                        );

                if (chunks != null) {
                    relatedChunks.addAll(chunks);
                }

            } catch (Exception exception) {
                System.out.println("[RevisionPackService] Related chunk retrieval failed for topic: "
                        + topic);
                System.out.println("[RevisionPackService] Error class = "
                        + exception.getClass().getName());
                System.out.println("[RevisionPackService] Error message = "
                        + exception.getMessage());
            }
        }

        return relatedChunks;
    }

    private String buildPrompt(
            WrongAnswersToolResult wrongAnswers,
            CourseProgressToolResult progress,
            List<LearningHistoryToolResult> learningHistory,
            List<NoteToolResult> notes,
            List<CourseDocumentToolResult> documents,
            List<DocumentSearchToolResult> relatedChunks
    ) {
        Map<String, Object> input =
                new LinkedHashMap<>();

        input.put(
                "wrongAnswers",
                simplifyWrongAnswers(wrongAnswers)
        );

        input.put(
                "progress",
                simplifyProgress(progress)
        );

        input.put(
                "learningHistory",
                simplifyLearningHistory(learningHistory)
        );

        input.put(
                "notes",
                simplifyNotes(notes)
        );

        input.put(
                "documents",
                simplifyDocuments(documents)
        );

        input.put(
                "relatedChunks",
                simplifyChunks(relatedChunks)
        );

        String jsonInput =
                toJson(
                        "revision-pack-prompt-input",
                        input
                );

        if (jsonInput.length() > 18000) {
            System.out.println("[RevisionPackService] jsonInput too long, truncate to 18000 chars.");
            jsonInput = jsonInput.substring(
                    0,
                    18000
            );
        }

        return """
                You are an AI study assistant for international students.
                Generate a personalized revision pack based on the student's real learning data.

                Return ONLY valid JSON.
                Do not include markdown.
                Do not include explanations outside JSON.

                JSON format:
                {
                  "title": "...",
                  "summary": "...",
                  "weakTopics": [
                    {
                      "topic": "...",
                      "reason": "..."
                    }
                  ],
                  "reviewOrder": ["..."],
                  "recommendedActions": ["..."],
                  "relatedDocuments": [
                    {
                      "documentId": 1,
                      "fileName": "lecture.pdf"
                    }
                  ],
                  "studyTasks": ["..."],
                  "suggestedFlashcards": ["..."]
                }

                Rules:
                - Base the revision pack on wrong answers, weak topics, progress, learning history, notes and retrieved chunks.
                - If wrong answers exist, prioritize unresolved wrong topics.
                - If weak topics are empty, create a general revision pack based on documents, progress and recent activity.
                - reviewOrder should be short and practical.
                - recommendedActions must be concrete actions the student can do next.
                - relatedDocuments must only use documents shown in the input.
                - Do not invent document ids.
                - Use simple English.

                Student learning data:
                %s
                """.formatted(jsonInput);
    }

    private Map<String, Object> simplifyWrongAnswers(
            WrongAnswersToolResult wrongAnswers
    ) {
        Map<String, Object> result =
                new LinkedHashMap<>();

        if (wrongAnswers == null) {
            result.put("weakTopics", List.of());
            result.put("wrongAnswerItems", List.of());
            return result;
        }

        result.put(
                "weakTopics",
                wrongAnswers.getWeakTopics() == null
                        ? List.of()
                        : wrongAnswers.getWeakTopics()
        );

        List<Map<String, Object>> items =
                new ArrayList<>();

        if (wrongAnswers.getWrongAnswerItems() != null) {
            for (WrongAnswerItem item :
                    wrongAnswers.getWrongAnswerItems()) {
                if (item == null) {
                    continue;
                }

                Map<String, Object> map =
                        new LinkedHashMap<>();

                map.put("wrongAnswerId", item.getWrongAnswerId());
                map.put("quizId", item.getQuizId());
                map.put("questionId", item.getQuestionId());
                map.put("topic", item.getTopic());
                map.put("userAnswer", item.getUserAnswer());
                map.put("correctAnswer", item.getCorrectAnswer());
                map.put("explanation", item.getExplanation());
                map.put("resolved", item.getResolved());
                map.put(
                        "createdAt",
                        item.getCreatedAt() == null
                                ? null
                                : item.getCreatedAt().toString()
                );

                items.add(map);
            }
        }

        result.put(
                "wrongAnswerItems",
                items
        );

        return result;
    }

    private Map<String, Object> simplifyProgress(
            CourseProgressToolResult progress
    ) {
        Map<String, Object> result =
                new LinkedHashMap<>();

        if (progress == null) {
            return result;
        }

        result.put("progressScore", progress.getProgressScore());
        result.put("averageQuizScore", progress.getAverageQuizScore());
        result.put("wrongAnswerCount", progress.getWrongAnswerCount());
        result.put("unresolvedWrongAnswerCount", progress.getUnresolvedWrongAnswerCount());

        result.put(
                "weakTopics",
                progress.getWeakTopics() == null
                        ? List.of()
                        : progress.getWeakTopics()
        );

        result.put(
                "recommendedNextReview",
                progress.getRecommendedNextReview()
        );

        result.put(
                "recentActivity",
                simplifyLearningHistory(
                        progress.getRecentActivity()
                )
        );

        return result;
    }

    private List<Map<String, Object>> simplifyLearningHistory(
            List<LearningHistoryToolResult> learningHistory
    ) {
        List<Map<String, Object>> result =
                new ArrayList<>();

        if (learningHistory == null) {
            return result;
        }

        for (LearningHistoryToolResult item : learningHistory) {
            if (item == null) {
                continue;
            }

            Map<String, Object> map =
                    new LinkedHashMap<>();

            map.put("eventType", item.getEventType());
            map.put("targetType", item.getTargetType());
            map.put("targetId", item.getTargetId());
            map.put("topic", item.getTopic());

            map.put(
                    "createdAt",
                    item.getCreatedAt() == null
                            ? null
                            : item.getCreatedAt().toString()
            );

            result.add(map);
        }

        return result;
    }

    private List<Map<String, Object>> simplifyNotes(
            List<NoteToolResult> notes
    ) {
        List<Map<String, Object>> result =
                new ArrayList<>();

        if (notes == null) {
            return result;
        }

        for (NoteToolResult note : notes) {
            if (note == null) {
                continue;
            }

            Map<String, Object> map =
                    new LinkedHashMap<>();

            map.put("noteId", note.getNoteId());
            map.put("title", note.getTitle());

            String content =
                    note.getContent();

            if (content != null && content.length() > 600) {
                content = content.substring(
                        0,
                        600
                );
            }

            map.put("content", content);
            map.put("topic", note.getTopic());
            map.put("documentId", note.getDocumentId());

            map.put(
                    "createdAt",
                    note.getCreatedAt() == null
                            ? null
                            : note.getCreatedAt().toString()
            );

            result.add(map);
        }

        return result;
    }

    private List<Map<String, Object>> simplifyDocuments(
            List<CourseDocumentToolResult> documents
    ) {
        List<Map<String, Object>> result =
                new ArrayList<>();

        if (documents == null) {
            return result;
        }

        for (CourseDocumentToolResult document : documents) {
            if (document == null) {
                continue;
            }

            Map<String, Object> map =
                    new LinkedHashMap<>();

            map.put("documentId", document.getDocumentId());
            map.put("fileName", document.getOriginalFileName());
            map.put("documentType", document.getDocumentType());
            map.put("status", document.getStatus());
            map.put("chunkCount", document.getChunkCount());

            map.put(
                    "processedAt",
                    document.getProcessedAt() == null
                            ? null
                            : document.getProcessedAt().toString()
            );

            result.add(map);
        }

        return result;
    }

    private List<Map<String, Object>> simplifyChunks(
            List<DocumentSearchToolResult> chunks
    ) {
        List<Map<String, Object>> result =
                new ArrayList<>();

        if (chunks == null) {
            return result;
        }

        for (DocumentSearchToolResult chunk : chunks) {
            if (chunk == null) {
                continue;
            }

            Map<String, Object> item =
                    new LinkedHashMap<>();

            item.put("chunkId", chunk.getChunkId());
            item.put("documentId", chunk.getDocumentId());
            item.put("fileName", chunk.getFileName());
            item.put("pageNumber", chunk.getPageNumber());
            item.put("sectionTitle", chunk.getSectionTitle());
            item.put("score", chunk.getScore());

            String content =
                    chunk.getContent();

            if (content != null && content.length() > 600) {
                content = content.substring(
                        0,
                        600
                );
            }

            item.put("content", content);

            result.add(item);
        }

        return result;
    }

    private RevisionPackResult parseResult(
            String rawContent
    ) {
        if (rawContent == null || rawContent.isBlank()) {
            throw new BusinessException(
                    "EMPTY_AI_OUTPUT",
                    "AI returned empty revision pack."
            );
        }

        String json =
                extractJson(rawContent);

        try {
            return objectMapper.readValue(
                    json,
                    RevisionPackResult.class
            );

        } catch (JsonProcessingException exception) {
            System.out.println("[RevisionPackService] Failed to parse LLM JSON.");
            System.out.println("[RevisionPackService] Error message = "
                    + exception.getMessage());
            System.out.println("[RevisionPackService] JSON content = "
                    + json);

            throw new BusinessException(
                    "INVALID_AI_JSON",
                    "AI revision pack output is not valid JSON."
            );
        }
    }

    private String extractJson(
            String rawContent
    ) {
        String text =
                rawContent.trim();

        if (text.startsWith("```")) {
            text = text
                    .replace("```json", "")
                    .replace("```", "")
                    .trim();
        }

        int start =
                text.indexOf("{");

        int end =
                text.lastIndexOf("}");

        if (start < 0 || end < start) {
            throw new BusinessException(
                    "INVALID_AI_JSON",
                    "AI output does not contain a JSON object."
            );
        }

        return text.substring(
                start,
                end + 1
        );
    }

    private RevisionPack toEntity(
            Long userId,
            Long courseId,
            RevisionPackResult result
    ) {
        RevisionPack pack =
                new RevisionPack();

        pack.setUserId(userId);
        pack.setCourseId(courseId);
        pack.setTitle(result.getTitle());
        pack.setSummary(result.getSummary());

        pack.setWeakTopicsJson(
                toJson(
                        "weakTopicsJson",
                        result.getWeakTopics()
                )
        );

        pack.setReviewOrderJson(
                toJson(
                        "reviewOrderJson",
                        result.getReviewOrder()
                )
        );

        pack.setRecommendedActionsJson(
                toJson(
                        "recommendedActionsJson",
                        result.getRecommendedActions()
                )
        );

        pack.setRelatedDocumentsJson(
                toJson(
                        "relatedDocumentsJson",
                        result.getRelatedDocuments()
                )
        );

        pack.setStudyTasksJson(
                toJson(
                        "studyTasksJson",
                        result.getStudyTasks()
                )
        );

        pack.setSuggestedFlashcardsJson(
                toJson(
                        "suggestedFlashcardsJson",
                        result.getSuggestedFlashcards()
                )
        );

        pack.setGeneratedQuizId(null);
        pack.setCreatedAt(LocalDateTime.now());

        return pack;
    }

    private RevisionPackResponse toResponse(
            RevisionPack pack
    ) {
        return new RevisionPackResponse(
                pack.getId(),
                pack.getCourseId(),
                pack.getTitle(),
                pack.getSummary(),
                fromWeakTopicJson(
                        pack.getWeakTopicsJson()
                ),
                fromStringListJson(
                        pack.getReviewOrderJson()
                ),
                fromStringListJson(
                        pack.getRecommendedActionsJson()
                ),
                fromRelatedDocumentJson(
                        pack.getRelatedDocumentsJson()
                ),
                fromStringListJson(
                        pack.getStudyTasksJson()
                ),
                fromStringListJson(
                        pack.getSuggestedFlashcardsJson()
                ),
                pack.getGeneratedQuizId(),
                pack.getCreatedAt()
        );
    }

    private void insertLearningHistorySafely(
            Long userId,
            Long courseId,
            Long revisionPackId
    ) {
        LocalDateTime now =
                LocalDateTime.now();

        try {
            revisionPackMapper.insertLearningHistory(
                    userId,
                    courseId,
                    "REVISION_PACK",
                    "COURSE",
                    courseId,
                    null,
                    now
            );

            System.out.println("[RevisionPackService] learning_history inserted with REVISION_PACK.");
            return;

        } catch (Exception exception) {
            System.out.println("[RevisionPackService] Failed to insert REVISION_PACK learning_history.");
            System.out.println("[RevisionPackService] Try fallback event_type = REVIEW.");
            System.out.println("[RevisionPackService] Error message = "
                    + exception.getMessage());
        }

        try {
            revisionPackMapper.insertLearningHistory(
                    userId,
                    courseId,
                    "REVIEW",
                    "COURSE",
                    courseId,
                    null,
                    now
            );

            System.out.println("[RevisionPackService] learning_history inserted with REVIEW.");
            return;

        } catch (Exception exception) {
            System.out.println("[RevisionPackService] Failed to insert REVIEW learning_history.");
            System.out.println("[RevisionPackService] Try fallback event_type = NOTE.");
            System.out.println("[RevisionPackService] Error message = "
                    + exception.getMessage());
        }

        try {
            revisionPackMapper.insertLearningHistory(
                    userId,
                    courseId,
                    "NOTE",
                    "COURSE",
                    courseId,
                    null,
                    now
            );

            System.out.println("[RevisionPackService] learning_history inserted with NOTE.");

        } catch (Exception exception) {
            System.out.println("[RevisionPackService] Failed to insert fallback learning_history.");
            System.out.println("[RevisionPackService] Skip learning_history for revision pack.");
            System.out.println("[RevisionPackService] Error message = "
                    + exception.getMessage());
        }
    }

    private void validateCurrentUser(
            Long currentUserId
    ) {
        if (currentUserId == null) {
            throw new BusinessException(
                    "UNAUTHORIZED",
                    "Current user is required."
            );
        }
    }

    private void validateCourseId(
            Long courseId
    ) {
        if (courseId == null) {
            throw new BusinessException(
                    "INVALID_COURSE_ID",
                    "Course id is required."
            );
        }
    }

    private void ensureCourseAccess(
            Long userId,
            Long courseId
    ) {
        int count =
                revisionPackMapper.countCourseOwnership(
                        userId,
                        courseId
                );

        if (count == 0) {
            throw new BusinessException(
                    "COURSE_NOT_FOUND",
                    "Course not found or access denied."
            );
        }
    }

    private int normalizeMaxWeakTopics(
            Integer maxWeakTopics
    ) {
        if (maxWeakTopics == null
                || maxWeakTopics <= 0) {
            return 5;
        }

        return Math.min(
                maxWeakTopics,
                10
        );
    }

    private int normalizeMaxRelatedChunks(
            Integer maxRelatedChunks
    ) {
        if (maxRelatedChunks == null
                || maxRelatedChunks <= 0) {
            return 3;
        }

        return Math.min(
                maxRelatedChunks,
                5
        );
    }

    private String toJson(
            String label,
            Object value
    ) {
        try {
            System.out.println("[RevisionPackService] Serializing JSON: "
                    + label);

            String json =
                    objectMapper.writeValueAsString(
                            value
                    );

            System.out.println("[RevisionPackService] JSON serialized successfully: "
                    + label);

            return json;

        } catch (JsonProcessingException exception) {
            System.out.println("[RevisionPackService] JSON serialization failed at: "
                    + label);
            System.out.println("[RevisionPackService] Error class = "
                    + exception.getClass().getName());
            System.out.println("[RevisionPackService] Error message = "
                    + exception.getMessage());

            throw new BusinessException(
                    "JSON_SERIALIZATION_FAILED",
                    "Failed to serialize revision pack data at "
                            + label
                            + ": "
                            + exception.getOriginalMessage()
            );
        }
    }

    private List<String> fromStringListJson(
            String json
    ) {
        if (json == null || json.isBlank()) {
            return List.of();
        }

        try {
            return objectMapper.readValue(
                    json,
                    new TypeReference<List<String>>() {
                    }
            );

        } catch (JsonProcessingException exception) {
            return List.of();
        }
    }

    private List<RevisionWeakTopicResult> fromWeakTopicJson(
            String json
    ) {
        if (json == null || json.isBlank()) {
            return List.of();
        }

        try {
            return objectMapper.readValue(
                    json,
                    new TypeReference<List<RevisionWeakTopicResult>>() {
                    }
            );

        } catch (JsonProcessingException exception) {
            return List.of();
        }
    }

    private List<RevisionRelatedDocumentResult> fromRelatedDocumentJson(
            String json
    ) {
        if (json == null || json.isBlank()) {
            return List.of();
        }

        try {
            return objectMapper.readValue(
                    json,
                    new TypeReference<List<RevisionRelatedDocumentResult>>() {
                    }
            );

        } catch (JsonProcessingException exception) {
            return List.of();
        }
    }
}