package com.msc.springai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.msc.springai.constant.AiWorkflowTypes;
import com.msc.springai.dto.learning.draft.SummaryDraftValue;
import com.msc.springai.dto.learning.request.SaveDraftRequest;
import com.msc.springai.dto.learning.request.SummaryGenerateRequest;
import com.msc.springai.dto.learning.response.SavedSummaryResponse;
import com.msc.springai.dto.learning.response.SummaryGenerateResponse;
import com.msc.springai.dto.learning.response.SummarySaveResponse;
import com.msc.springai.dto.learning.result.SummaryResult;
import com.msc.springai.dto.learning.retrieval.RetrievedChunk;
import com.msc.springai.entity.Course;
import com.msc.springai.entity.CourseDocument;
import com.msc.springai.entity.Summary;
import com.msc.springai.exception.BusinessException;
import com.msc.springai.mapper.CourseDocumentMapper;
import com.msc.springai.mapper.CourseMapper;
import com.msc.springai.mapper.LearningHistoryMapper;
import com.msc.springai.mapper.SummaryMapper;
import com.msc.springai.service.observability.AiChatResponseUtil;
import com.msc.springai.service.observability.AiRequestLogContext;
import com.msc.springai.service.observability.AiRequestLogService;
import com.msc.springai.service.prompt.SummaryPromptBuilder;
import com.msc.springai.service.validator.SummaryOutputValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
@RequiredArgsConstructor
public class SummaryService {

    private static final String SOURCE_SCOPE_COURSE = "COURSE";
    private static final String SOURCE_SCOPE_DOCUMENT = "DOCUMENT";

    private final RetrievalService retrievalService;

    private final DraftCacheService draftCacheService;

    private final SummaryOutputValidator summaryOutputValidator;

    private final SummaryPromptBuilder summaryPromptBuilder;

    private final SummaryMapper summaryMapper;

    private final CourseMapper courseMapper;

    private final CourseDocumentMapper courseDocumentMapper;

    private final LearningHistoryMapper learningHistoryMapper;

    private final ObjectMapper objectMapper;

    private final ChatClient.Builder chatClientBuilder;

    private final AiRequestLogService aiRequestLogService;

    public SummaryGenerateResponse generateCourseSummary(
            Long userId,
            Long courseId,
            SummaryGenerateRequest request
    ) {
        System.out.println("[SummaryService] Start course summary generation.");
        System.out.println("[SummaryService] userId = " + userId);
        System.out.println("[SummaryService] courseId = " + courseId);

        AiRequestLogContext logContext =
                aiRequestLogService.start(
                        userId,
                        courseId,
                        AiWorkflowTypes.SUMMARY
                );

        try {
            int topK =
                    normalizeTopK(
                            request == null
                                    ? null
                                    : request.getTopK()
                    );

            String retrievalQuery =
                    request == null
                            ? null
                            : request.getRetrievalQuery();

            List<RetrievedChunk> chunks =
                    retrievalService.retrieveCourseChunks(
                            userId,
                            courseId,
                            topK,
                            retrievalQuery
                    );

            aiRequestLogService.setRetrievedChunkCount(
                    logContext,
                    chunks == null
                            ? 0
                            : chunks.size()
            );

            SummaryResult result =
                    generateSummaryFromChunks(
                            SOURCE_SCOPE_COURSE,
                            chunks,
                            logContext
                    );

            SummaryDraftValue draftValue =
                    new SummaryDraftValue(
                            userId,
                            courseId,
                            null,
                            SOURCE_SCOPE_COURSE,
                            result
                    );

            String draftKey =
                    draftCacheService.buildSummaryDraftKey(
                            userId,
                            courseId,
                            SOURCE_SCOPE_COURSE,
                            buildDraftParams(
                                    null,
                                    topK,
                                    retrievalQuery
                            )
                    );

            draftCacheService.saveDraft(
                    draftKey,
                    draftValue
            );

            aiRequestLogService.completeSuccess(
                    logContext
            );

            System.out.println("[SummaryService] Course summary draft saved.");
            System.out.println("[SummaryService] draftKey = " + draftKey);

            return toGenerateResponse(
                    draftKey,
                    result
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
                    "SUMMARY_GENERATION_FAILED",
                    "Failed to generate course summary. Please try again."
            );
        }
    }

    public SummaryGenerateResponse generateDocumentSummary(
            Long userId,
            Long documentId,
            SummaryGenerateRequest request
    ) {
        System.out.println("[SummaryService] Start document summary generation.");
        System.out.println("[SummaryService] userId = " + userId);
        System.out.println("[SummaryService] documentId = " + documentId);

        CourseDocument document =
                courseDocumentMapper.findByIdAndUserId(
                        documentId,
                        userId
                );

        if (document == null) {
            throw new BusinessException(
                    "DOCUMENT_NOT_FOUND",
                    "Document not found or access denied."
            );
        }

        Long courseId = document.getCourseId();

        AiRequestLogContext logContext =
                aiRequestLogService.start(
                        userId,
                        courseId,
                        AiWorkflowTypes.SUMMARY
                );

        try {
            int topK =
                    normalizeTopK(
                            request == null
                                    ? null
                                    : request.getTopK()
                    );

            String retrievalQuery =
                    request == null
                            ? null
                            : request.getRetrievalQuery();

            List<RetrievedChunk> chunks =
                    retrievalService.retrieveDocumentChunks(
                            userId,
                            courseId,
                            documentId,
                            topK,
                            retrievalQuery
                    );

            aiRequestLogService.setRetrievedChunkCount(
                    logContext,
                    chunks == null
                            ? 0
                            : chunks.size()
            );

            SummaryResult result =
                    generateSummaryFromChunks(
                            SOURCE_SCOPE_DOCUMENT,
                            chunks,
                            logContext
                    );

            SummaryDraftValue draftValue =
                    new SummaryDraftValue(
                            userId,
                            courseId,
                            documentId,
                            SOURCE_SCOPE_DOCUMENT,
                            result
                    );

            String draftKey =
                    draftCacheService.buildSummaryDraftKey(
                            userId,
                            courseId,
                            SOURCE_SCOPE_DOCUMENT,
                            buildDraftParams(
                                    documentId,
                                    topK,
                                    retrievalQuery
                            )
                    );

            draftCacheService.saveDraft(
                    draftKey,
                    draftValue
            );

            aiRequestLogService.completeSuccess(
                    logContext
            );

            System.out.println("[SummaryService] Document summary draft saved.");
            System.out.println("[SummaryService] draftKey = " + draftKey);

            return toGenerateResponse(
                    draftKey,
                    result
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
                    "SUMMARY_GENERATION_FAILED",
                    "Failed to generate document summary. Please try again."
            );
        }
    }

    public SummarySaveResponse saveSummary(
            Long userId,
            SaveDraftRequest request
    ) {
        System.out.println("[SummaryService] Start save summary.");

        if (request == null
                || request.getDraftKey() == null
                || request.getDraftKey().isBlank()) {
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

        SummaryDraftValue draft =
                draftCacheService.getDraft(
                        draftKey,
                        SummaryDraftValue.class
                );

        validateDraftValue(
                userId,
                draft
        );

        SummaryResult result = draft.getResult();

        summaryOutputValidator.validate(result);

        Summary summary = new Summary();

        summary.setUserId(userId);
        summary.setCourseId(draft.getCourseId());
        summary.setDocumentId(draft.getDocumentId());
        summary.setTitle(result.getTitle());
        summary.setSummary(result.getSummary());

        summary.setKeyConceptsJson(
                writeJson(result.getKeyConcepts())
        );

        summary.setDefinitionsJson(
                writeJson(result.getDefinitions())
        );

        summary.setRevisionNotes(result.getRevisionNotes());
        summary.setSourceScope(draft.getSourceScope());

        summaryMapper.insert(summary);

        if (summary.getId() == null) {
            throw new BusinessException(
                    "SUMMARY_SAVE_FAILED",
                    "Failed to save summary."
            );
        }

        learningHistoryMapper.insertLearningHistory(
                userId,
                draft.getCourseId(),
                "SUMMARY",
                "SUMMARY",
                summary.getId(),
                summary.getTitle()
        );

        draftCacheService.deleteDraft(draftKey);

        System.out.println("[SummaryService] Summary saved.");
        System.out.println("[SummaryService] summaryId = " + summary.getId());

        return new SummarySaveResponse(
                summary.getId()
        );
    }

    public List<SavedSummaryResponse> getCourseSummaries(
            Long userId,
            Long courseId
    ) {
        System.out.println("[SummaryService] Get course summaries.");
        System.out.println("[SummaryService] userId = " + userId);
        System.out.println("[SummaryService] courseId = " + courseId);

        Course course =
                courseMapper.findByIdAndUserId(
                        courseId,
                        userId
                );

        if (course == null) {
            throw new BusinessException(
                    "COURSE_NOT_FOUND",
                    "Course not found or access denied."
            );
        }

        List<Summary> summaries =
                summaryMapper.findByUserIdAndCourseId(
                        userId,
                        courseId
                );

        List<SavedSummaryResponse> responses =
                new ArrayList<>();

        for (Summary summary : summaries) {
            responses.add(
                    toSavedSummaryResponse(summary)
            );
        }

        return responses;
    }

    public void deleteSummary(
            Long userId,
            Long summaryId
    ) {
        System.out.println("[SummaryService] Delete summary.");
        System.out.println("[SummaryService] userId = " + userId);
        System.out.println("[SummaryService] summaryId = " + summaryId);

        Summary existing =
                summaryMapper.findByIdAndUserId(
                        summaryId,
                        userId
                );

        if (existing == null) {
            throw new BusinessException(
                    "SUMMARY_NOT_FOUND",
                    "Summary not found or access denied."
            );
        }

        int deleted =
                summaryMapper.deleteByIdAndUserId(
                        summaryId,
                        userId
                );

        if (deleted <= 0) {
            throw new BusinessException(
                    "SUMMARY_DELETE_FAILED",
                    "Failed to delete summary."
            );
        }

        System.out.println("[SummaryService] Summary deleted.");
    }

    private SummaryResult generateSummaryFromChunks(
            String sourceScope,
            List<RetrievedChunk> chunks,
            AiRequestLogContext logContext
    ) {
        String prompt =
                summaryPromptBuilder.buildSummaryPrompt(
                        sourceScope,
                        chunks
                );

        SummaryResult result =
                callLlmForSummary(
                        prompt,
                        logContext
                );

        /*
         * sourceScope 是后端确定的，
         * 不让 LLM 决定。
         */
        result.setSourceScope(sourceScope);

        summaryOutputValidator.validate(result);

        return result;
    }

    private SummaryResult callLlmForSummary(
            String prompt,
            AiRequestLogContext logContext
    ) {
        System.out.println(
                "[SummaryService] Start calling LLM for summary."
        );

        System.out.println(
                "[SummaryService] prompt length = "
                        + (
                        prompt == null
                                ? 0
                                : prompt.length()
                )
        );

        try {
            System.out.println(
                    "[SummaryService] About to call LLM."
            );

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

            String raw =
                    AiChatResponseUtil.extractText(
                            chatResponse
                    );

            System.out.println(
                    "[SummaryService] LLM returned raw response."
            );

            System.out.println(
                    "[SummaryService] raw length = "
                            + (
                            raw == null
                                    ? 0
                                    : raw.length()
                    )
            );

            System.out.println(
                    "[SummaryService] raw preview = "
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

            if (raw == null || raw.isBlank()) {
                throw new BusinessException(
                        "AI_GENERATION_FAILED",
                        "LLM returned empty summary."
                );
            }

            String json =
                    extractJson(raw);

            SummaryResult result =
                    objectMapper.readValue(
                            json,
                            SummaryResult.class
                    );

            System.out.println(
                    "[SummaryService] Summary JSON parsed."
            );

            return result;

        } catch (BusinessException exception) {
            throw exception;

        } catch (Exception exception) {
            System.out.println(
                    "[SummaryService] Failed to generate summary."
            );

            System.out.println(
                    "[SummaryService] exception class = "
                            + exception
                            .getClass()
                            .getName()
            );

            System.out.println(
                    "[SummaryService] error message = "
                            + exception.getMessage()
            );

            Throwable cause = exception.getCause();

            int level = 1;

            while (cause != null && level <= 5) {
                System.out.println(
                        "[SummaryService] cause "
                                + level
                                + " class = "
                                + cause
                                .getClass()
                                .getName()
                );

                System.out.println(
                        "[SummaryService] cause "
                                + level
                                + " message = "
                                + cause.getMessage()
                );

                cause = cause.getCause();
                level++;
            }

            throw new BusinessException(
                    "AI_GENERATION_FAILED",
                    "Failed to generate summary. Please try again."
            );
        }
    }

    private String extractJson(
            String raw
    ) {
        String text = raw.trim();

        if (text.startsWith("```")) {
            text = text
                    .replaceFirst("^```json", "")
                    .replaceFirst("^```", "")
                    .replaceFirst("```$", "")
                    .trim();
        }

        int start = text.indexOf("{");
        int end = text.lastIndexOf("}");

        if (start < 0 || end < 0 || end <= start) {
            throw new BusinessException(
                    "AI_OUTPUT_INVALID",
                    "Generated summary is invalid. Please regenerate."
            );
        }

        return text.substring(
                start,
                end + 1
        );
    }

    private void validateDraftValue(
            Long userId,
            SummaryDraftValue draft
    ) {
        if (draft == null) {
            throw new BusinessException(
                    "DRAFT_NOT_FOUND",
                    "Draft does not exist or has expired. Please generate again."
            );
        }

        if (!userId.equals(draft.getUserId())) {
            throw new BusinessException(
                    "FORBIDDEN_DRAFT",
                    "You cannot use another user's draft."
            );
        }

        Course course =
                courseMapper.findByIdAndUserId(
                        draft.getCourseId(),
                        userId
                );

        if (course == null) {
            throw new BusinessException(
                    "COURSE_NOT_FOUND",
                    "Course not found or access denied."
            );
        }

        if (SOURCE_SCOPE_DOCUMENT.equals(draft.getSourceScope())) {
            CourseDocument document =
                    courseDocumentMapper.findByIdAndUserId(
                            draft.getDocumentId(),
                            userId
                    );

            if (document == null) {
                throw new BusinessException(
                        "DOCUMENT_NOT_FOUND",
                        "Document not found or access denied."
                );
            }

            if (!draft.getCourseId().equals(document.getCourseId())) {
                throw new BusinessException(
                        "DOCUMENT_ACCESS_DENIED",
                        "Document does not belong to this course."
                );
            }
        }
    }

    private String writeJson(
            Object value
    ) {
        try {
            return objectMapper.writeValueAsString(
                    value
            );

        } catch (Exception exception) {
            throw new BusinessException(
                    "JSON_SERIALIZE_FAILED",
                    "Failed to serialize summary content."
            );
        }
    }

    private Map<String, Object> buildDraftParams(
            Long documentId,
            Integer topK,
            String retrievalQuery
    ) {
        Map<String, Object> params =
                new TreeMap<>();

        if (documentId != null) {
            params.put(
                    "documentId",
                    documentId
            );
        }

        params.put(
                "topK",
                topK
        );

        if (retrievalQuery != null
                && !retrievalQuery.isBlank()) {
            params.put(
                    "retrievalQuery",
                    retrievalQuery.trim()
            );
        }

        return params;
    }

    private int normalizeTopK(
            Integer topK
    ) {
        if (topK == null) {
            return 3;
        }

        if (topK < 1) {
            return 1;
        }

        return Math.min(
                topK,
                8
        );
    }

    private SummaryGenerateResponse toGenerateResponse(
            String draftKey,
            SummaryResult result
    ) {
        return new SummaryGenerateResponse(
                draftKey,
                result.getTitle(),
                result.getSummary(),
                result.getKeyConcepts(),
                result.getDefinitions(),
                result.getRevisionNotes(),
                result.getSourceScope()
        );
    }

    private SavedSummaryResponse toSavedSummaryResponse(
            Summary summary
    ) {
        return new SavedSummaryResponse(
                summary.getId(),
                summary.getUserId(),
                summary.getCourseId(),
                summary.getDocumentId(),
                summary.getTitle(),
                summary.getSummary(),
                summary.getKeyConceptsJson(),
                summary.getDefinitionsJson(),
                summary.getRevisionNotes(),
                summary.getSourceScope(),
                summary.getCreatedAt()
        );
    }
}