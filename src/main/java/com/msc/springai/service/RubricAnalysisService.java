package com.msc.springai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.msc.springai.constant.AiWorkflowTypes;
import com.msc.springai.dto.learning.retrieval.RetrievedChunk;
import com.msc.springai.dto.workflow.rubric.RubricAnalysisResponse;
import com.msc.springai.dto.workflow.rubric.RubricAnalysisResult;
import com.msc.springai.dto.workflow.rubric.RubricAnalyzeRequest;
import com.msc.springai.dto.workflow.rubric.RubricCriterionResult;
import com.msc.springai.entity.CourseDocument;
import com.msc.springai.entity.RubricAnalysis;
import com.msc.springai.exception.BusinessException;
import com.msc.springai.mapper.CourseDocumentMapper;
import com.msc.springai.mapper.RubricAnalysisMapper;
import com.msc.springai.service.observability.AiChatResponseUtil;
import com.msc.springai.service.observability.AiRequestLogContext;
import com.msc.springai.service.observability.AiRequestLogService;
import com.msc.springai.service.validator.RubricAnalysisValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RubricAnalysisService {

    private static final String DOCUMENT_STATUS_READY = "READY";
    private static final String DOCUMENT_TYPE_RUBRIC = "RUBRIC";

    private final CourseDocumentMapper courseDocumentMapper;
    private final RubricAnalysisMapper rubricAnalysisMapper;
    private final RetrievalService retrievalService;
    private final RubricAnalysisValidator rubricAnalysisValidator;
    private final ObjectMapper objectMapper;
    private final ChatClient.Builder chatClientBuilder;
    private final AiRequestLogService aiRequestLogService;

    public RubricAnalysisResponse analyzeRubric(
            Long currentUserId,
            Long documentId,
            RubricAnalyzeRequest request
    ) {
        if (currentUserId == null) {
            throw new BusinessException(
                    "UNAUTHORIZED",
                    "Current user is required."
            );
        }

        if (documentId == null) {
            throw new BusinessException(
                    "INVALID_DOCUMENT_ID",
                    "Document id is required."
            );
        }

        CourseDocument document =
                courseDocumentMapper.findByIdAndUserId(
                        documentId,
                        currentUserId
                );

        if (document == null) {
            throw new BusinessException(
                    "DOCUMENT_NOT_FOUND",
                    "Document not found or access denied."
            );
        }

        validateDocument(
                document,
                request
        );

        AiRequestLogContext logContext =
                aiRequestLogService.start(
                        currentUserId,
                        document.getCourseId(),
                        AiWorkflowTypes.RUBRIC_ANALYSIS
                );

        try {
            List<RetrievedChunk> chunks =
                    retrievalService.retrieveDocumentChunks(
                            currentUserId,
                            document.getCourseId(),
                            documentId,
                            12,
                            "Extract rubric marking criteria, grade bands, excellent requirements, common mistakes and high score strategy."
                    );

            aiRequestLogService.setRetrievedChunkCount(
                    logContext,
                    chunks == null
                            ? 0
                            : chunks.size()
            );

            String context =
                    buildContext(chunks);

            System.out.println(
                    "[RubricAnalysisService] context length = "
                            + context.length()
            );

            String prompt =
                    buildPrompt(context);

            System.out.println(
                    "[RubricAnalysisService] prompt length = "
                            + prompt.length()
            );

            System.out.println(
                    "[RubricAnalysisService] Start LLM rubric analysis."
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

            String rawContent =
                    AiChatResponseUtil.extractText(
                            chatResponse
                    );

            System.out.println(
                    "[RubricAnalysisService] LLM raw content:"
            );

            System.out.println(rawContent);

            RubricAnalysisResult result =
                    parseResult(rawContent);

            rubricAnalysisValidator.validate(result);

            RubricAnalysis analysis =
                    toEntity(
                            currentUserId,
                            document.getCourseId(),
                            documentId,
                            result
                    );

            rubricAnalysisMapper.insert(analysis);

            rubricAnalysisMapper.insertLearningHistory(
                    currentUserId,
                    document.getCourseId(),
                    "RUBRIC_ANALYSIS",
                    "DOCUMENT",
                    documentId,
                    null,
                    LocalDateTime.now()
            );

            aiRequestLogService.completeSuccess(
                    logContext
            );

            return toResponse(analysis);

        } catch (BusinessException exception) {
            aiRequestLogService.completeFailure(
                    logContext,
                    exception
            );

            throw exception;

        } catch (Exception exception) {
            System.out.println(
                    "[RubricAnalysisService] Rubric analysis failed."
            );

            System.out.println(
                    "[RubricAnalysisService] exception class = "
                            + exception
                            .getClass()
                            .getName()
            );

            System.out.println(
                    "[RubricAnalysisService] error message = "
                            + exception.getMessage()
            );

            aiRequestLogService.completeFailure(
                    logContext,
                    exception
            );

            throw new BusinessException(
                    "RUBRIC_ANALYSIS_FAILED",
                    "Failed to generate rubric analysis. Please try again."
            );
        }
    }

    public List<RubricAnalysisResponse> getCourseRubricAnalyses(
            Long currentUserId,
            Long courseId
    ) {
        if (currentUserId == null) {
            throw new BusinessException(
                    "UNAUTHORIZED",
                    "Current user is required."
            );
        }

        if (courseId == null) {
            throw new BusinessException(
                    "INVALID_COURSE_ID",
                    "Course id is required."
            );
        }

        int count =
                rubricAnalysisMapper.countCourseOwnership(
                        currentUserId,
                        courseId
                );

        if (count == 0) {
            throw new BusinessException(
                    "COURSE_NOT_FOUND",
                    "Course not found or access denied."
            );
        }

        return rubricAnalysisMapper
                .findByCourseIdAndUserId(
                        currentUserId,
                        courseId
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private void validateDocument(
            CourseDocument document,
            RubricAnalyzeRequest request
    ) {
        if (!DOCUMENT_STATUS_READY.equals(
                document.getStatus()
        )) {
            throw new BusinessException(
                    "DOCUMENT_NOT_READY",
                    "Document is not ready for rubric analysis."
            );
        }

        boolean force =
                request != null
                        && Boolean.TRUE.equals(
                        request.getForce()
                );

        if (!force
                && !DOCUMENT_TYPE_RUBRIC.equals(
                document.getDocumentType()
        )) {
            throw new BusinessException(
                    "INVALID_DOCUMENT_TYPE",
                    "Only RUBRIC documents can be analyzed as rubrics. Use force=true to override."
            );
        }
    }

    private String buildContext(
            List<RetrievedChunk> chunks
    ) {
        if (chunks == null || chunks.isEmpty()) {
            throw new BusinessException(
                    "NO_RELEVANT_CHUNKS",
                    "No relevant chunks found for rubric analysis."
            );
        }

        StringBuilder builder =
                new StringBuilder();

        for (RetrievedChunk chunk : chunks) {
            if (chunk == null
                    || chunk.getContent() == null
                    || chunk.getContent().isBlank()) {
                continue;
            }

            builder.append("\n[Source: ")
                    .append(chunk.getFileName())
                    .append(", page ")
                    .append(chunk.getPageNumber())
                    .append("]\n")
                    .append(chunk.getContent())
                    .append("\n");
        }

        String context =
                builder.toString();

        if (context.isBlank()) {
            throw new BusinessException(
                    "NO_RELEVANT_CHUNKS",
                    "No relevant chunks found for rubric analysis."
            );
        }

        if (context.length() > 15000) {
            return context.substring(
                    0,
                    15000
            );
        }

        return context;
    }

    private String buildPrompt(
            String context
    ) {
        return """
                You are an AI study assistant for international students.
                Analyze the following marking rubric.

                Return ONLY valid JSON.
                Do not include markdown.
                Do not include explanations outside JSON.

                JSON format:
                {
                  "criteria": [
                    {
                      "name": "...",
                      "weight": "...",
                      "description": "..."
                    }
                  ],
                  "excellentBand": ["..."],
                  "commonMistakes": "...",
                  "highScoreStrategy": "..."
                }

                Rules:
                - Only extract information clearly supported by the rubric content.
                - Do not invent marking weights if they are not stated. Use "Not specified" for unknown weight.
                - criteria must list the main grading criteria.
                - excellentBand must describe what is required for high marks or excellent performance.
                - commonMistakes must summarize likely ways students lose marks.
                - highScoreStrategy must explain how a student should align their work with the rubric.
                - Use simple English.
                - If the content is not a rubric, do not invent grading criteria.

                Rubric content:
                %s
                """.formatted(context);
    }

    private RubricAnalysisResult parseResult(
            String rawContent
    ) {
        if (rawContent == null || rawContent.isBlank()) {
            throw new BusinessException(
                    "EMPTY_AI_OUTPUT",
                    "AI returned empty rubric analysis."
            );
        }

        String json =
                extractJson(rawContent);

        try {
            return objectMapper.readValue(
                    json,
                    RubricAnalysisResult.class
            );

        } catch (JsonProcessingException exception) {
            throw new BusinessException(
                    "INVALID_AI_JSON",
                    "AI rubric analysis output is not valid JSON."
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

    private RubricAnalysis toEntity(
            Long userId,
            Long courseId,
            Long documentId,
            RubricAnalysisResult result
    ) {
        RubricAnalysis analysis =
                new RubricAnalysis();

        analysis.setUserId(userId);
        analysis.setCourseId(courseId);
        analysis.setDocumentId(documentId);

        analysis.setCriteriaJson(
                toJson(result.getCriteria())
        );

        analysis.setExcellentBandJson(
                toJson(result.getExcellentBand())
        );

        analysis.setCommonMistakes(
                result.getCommonMistakes()
        );

        analysis.setHighScoreStrategy(
                result.getHighScoreStrategy()
        );

        analysis.setCreatedAt(
                LocalDateTime.now()
        );

        return analysis;
    }

    private RubricAnalysisResponse toResponse(
            RubricAnalysis analysis
    ) {
        return new RubricAnalysisResponse(
                analysis.getId(),
                analysis.getCourseId(),
                analysis.getDocumentId(),
                fromCriteriaJson(
                        analysis.getCriteriaJson()
                ),
                fromStringListJson(
                        analysis.getExcellentBandJson()
                ),
                analysis.getCommonMistakes(),
                analysis.getHighScoreStrategy(),
                analysis.getCreatedAt()
        );
    }

    private String toJson(
            Object value
    ) {
        try {
            return objectMapper.writeValueAsString(
                    value
            );

        } catch (JsonProcessingException exception) {
            throw new BusinessException(
                    "JSON_SERIALIZATION_FAILED",
                    "Failed to serialize rubric analysis data."
            );
        }
    }

    private List<RubricCriterionResult> fromCriteriaJson(
            String json
    ) {
        if (json == null || json.isBlank()) {
            return List.of();
        }

        try {
            return objectMapper.readValue(
                    json,
                    new TypeReference<List<RubricCriterionResult>>() {
                    }
            );

        } catch (JsonProcessingException exception) {
            return List.of();
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
}