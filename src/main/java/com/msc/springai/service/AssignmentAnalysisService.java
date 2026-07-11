package com.msc.springai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.msc.springai.constant.AiWorkflowTypes;
import com.msc.springai.dto.learning.retrieval.RetrievedChunk;
import com.msc.springai.dto.workflow.assignment.AssignmentAnalysisResponse;
import com.msc.springai.dto.workflow.assignment.AssignmentAnalysisResult;
import com.msc.springai.dto.workflow.assignment.AssignmentAnalyzeRequest;
import com.msc.springai.entity.AssignmentAnalysis;
import com.msc.springai.entity.CourseDocument;
import com.msc.springai.exception.BusinessException;
import com.msc.springai.mapper.AssignmentAnalysisMapper;
import com.msc.springai.mapper.CourseDocumentMapper;
import com.msc.springai.service.observability.AiChatResponseUtil;
import com.msc.springai.service.observability.AiRequestLogContext;
import com.msc.springai.service.observability.AiRequestLogService;
import com.msc.springai.service.validator.AssignmentAnalysisValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AssignmentAnalysisService {
    private final ChatClient.Builder chatClientBuilder;

    private static final String DOCUMENT_STATUS_READY = "READY";
    private static final String DOCUMENT_TYPE_ASSIGNMENT_BRIEF = "ASSIGNMENT_BRIEF";

    private final CourseDocumentMapper courseDocumentMapper;
    private final AssignmentAnalysisMapper assignmentAnalysisMapper;
    private final RetrievalService retrievalService;
    private final AssignmentAnalysisValidator assignmentAnalysisValidator;
    private final ObjectMapper objectMapper;
    private final AiRequestLogService aiRequestLogService;

    public AssignmentAnalysisResponse analyzeAssignment(
            Long currentUserId,
            Long documentId,
            AssignmentAnalyzeRequest request
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

        validateDocument(document, request);

        List<RetrievedChunk> chunks =
                retrievalService.retrieveDocumentChunks(
                        currentUserId,
                        document.getCourseId(),
                        documentId,
                        12,
                        """
                        Extract assignment requirements, deliverables, deadline, \
                        checklist and high score advice from this assignment brief.
                        """
                );

        String context = buildContext(chunks);
        String prompt = buildPrompt(context);

        AiRequestLogContext logContext =
                aiRequestLogService.start(
                        currentUserId,
                        document.getCourseId(),
                        AiWorkflowTypes.ASSIGNMENT_ANALYSIS
                );

        aiRequestLogService.setRetrievedChunkCount(
                logContext,
                chunks.size()
        );

        try {
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

            AssignmentAnalysisResult result =
                    parseResult(rawContent);

            assignmentAnalysisValidator.validate(result);

            AssignmentAnalysis analysis = toEntity(
                    currentUserId,
                    document.getCourseId(),
                    documentId,
                    result
            );

            assignmentAnalysisMapper.insert(analysis);

            assignmentAnalysisMapper.insertLearningHistory(
                    currentUserId,
                    document.getCourseId(),
                    "ASSIGNMENT_ANALYSIS",
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
            BusinessException wrappedException =
                    new BusinessException(
                            "ASSIGNMENT_ANALYSIS_FAILED",
                            "Failed to analyze assignment. Please try again."
                    );

            aiRequestLogService.completeFailure(
                    logContext,
                    exception
            );

            throw wrappedException;
        }
    }

    public List<AssignmentAnalysisResponse> getCourseAssignmentAnalyses(Long currentUserId,
                                                                        Long courseId) {
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

        int count = assignmentAnalysisMapper.countCourseOwnership(
                currentUserId,
                courseId
        );

        if (count == 0) {
            throw new BusinessException(
                    "COURSE_NOT_FOUND",
                    "Course not found or access denied."
            );
        }

        return assignmentAnalysisMapper
                .findByCourseIdAndUserId(currentUserId, courseId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private void validateDocument(CourseDocument document,
                                  AssignmentAnalyzeRequest request) {
        if (!DOCUMENT_STATUS_READY.equals(document.getStatus())) {
            throw new BusinessException(
                    "DOCUMENT_NOT_READY",
                    "Document is not ready for assignment analysis."
            );
        }

        boolean force = request != null && Boolean.TRUE.equals(request.getForce());

        if (!force && !DOCUMENT_TYPE_ASSIGNMENT_BRIEF.equals(document.getDocumentType())) {
            throw new BusinessException(
                    "INVALID_DOCUMENT_TYPE",
                    "Only ASSIGNMENT_BRIEF documents can be analyzed as assignment briefs. Use force=true to override."
            );
        }
    }

    private String buildContext(List<RetrievedChunk> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            throw new BusinessException(
                    "NO_RELEVANT_CHUNKS",
                    "No relevant chunks found for assignment analysis."
            );
        }

        StringBuilder builder = new StringBuilder();

        for (RetrievedChunk chunk : chunks) {
            if (chunk == null || chunk.getContent() == null || chunk.getContent().isBlank()) {
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

        String context = builder.toString();

        if (context.length() > 15000) {
            return context.substring(0, 15000);
        }

        return context;
    }

    private String buildPrompt(String context) {
        return """
                You are an AI study assistant for international students.
                Analyze the following assignment brief.

                Return ONLY valid JSON.
                Do not include markdown.
                Do not include explanations outside JSON.

                JSON format:
                {
                  "requirements": ["..."],
                  "deliverables": ["..."],
                  "deadline": null,
                  "checklist": ["..."],
                  "highScoreTips": "...",
                  "suggestedStructure": ["..."],
                  "riskWarnings": ["..."]
                }

                Rules:
                                     - requirements must list the main required work.
                                     - deliverables must list files, reports, code, demo, presentation or other outputs.
                                     - Only extract information that is clearly supported by the provided content.
                                     - Do not invent deliverables, deadlines, presentations, reports, or requirements if they are not stated in the content.
                                     - deadline can be null if not found.
                                     - checklist must be practical and action-based.
                                     - highScoreTips must explain how to improve marks.
                                     - suggestedStructure should suggest report or submission structure.
                                     - riskWarnings should list common missing parts or risks.
                                     - Use simple English.

                Assignment brief content:
                %s
                """.formatted(context);
    }

    private AssignmentAnalysisResult parseResult(String rawContent) {
        if (rawContent == null || rawContent.isBlank()) {
            throw new BusinessException(
                    "EMPTY_AI_OUTPUT",
                    "AI returned empty assignment analysis."
            );
        }

        String json = extractJson(rawContent);

        try {
            return objectMapper.readValue(json, AssignmentAnalysisResult.class);

        } catch (JsonProcessingException e) {
            throw new BusinessException(
                    "INVALID_AI_JSON",
                    "AI assignment analysis output is not valid JSON."
            );
        }
    }

    private String extractJson(String rawContent) {
        String text = rawContent.trim();

        if (text.startsWith("```")) {
            text = text.replace("```json", "")
                    .replace("```", "")
                    .trim();
        }

        int start = text.indexOf("{");
        int end = text.lastIndexOf("}");

        if (start < 0 || end < start) {
            throw new BusinessException(
                    "INVALID_AI_JSON",
                    "AI output does not contain a JSON object."
            );
        }

        return text.substring(start, end + 1);
    }

    private AssignmentAnalysis toEntity(Long userId,
                                        Long courseId,
                                        Long documentId,
                                        AssignmentAnalysisResult result) {
        AssignmentAnalysis analysis = new AssignmentAnalysis();

        analysis.setUserId(userId);
        analysis.setCourseId(courseId);
        analysis.setDocumentId(documentId);
        analysis.setRequirementsJson(toJson(result.getRequirements()));
        analysis.setDeliverablesJson(toJson(result.getDeliverables()));
        analysis.setDeadline(parseDeadline(result.getDeadline()));
        analysis.setChecklistJson(toJson(result.getChecklist()));
        analysis.setHighScoreTips(result.getHighScoreTips());
        analysis.setSuggestedStructureJson(toJson(result.getSuggestedStructure()));
        analysis.setRiskWarningsJson(toJson(result.getRiskWarnings()));
        analysis.setCreatedAt(LocalDateTime.now());

        return analysis;
    }

    private AssignmentAnalysisResponse toResponse(AssignmentAnalysis analysis) {
        return new AssignmentAnalysisResponse(
                analysis.getId(),
                analysis.getCourseId(),
                analysis.getDocumentId(),
                fromJsonList(analysis.getRequirementsJson()),
                fromJsonList(analysis.getDeliverablesJson()),
                analysis.getDeadline(),
                fromJsonList(analysis.getChecklistJson()),
                analysis.getHighScoreTips(),
                fromJsonList(analysis.getSuggestedStructureJson()),
                fromJsonList(analysis.getRiskWarningsJson()),
                analysis.getCreatedAt()
        );
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);

        } catch (JsonProcessingException e) {
            throw new BusinessException(
                    "JSON_SERIALIZATION_FAILED",
                    "Failed to serialize assignment analysis data."
            );
        }
    }

    private List<String> fromJsonList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }

        try {
            return objectMapper.readValue(
                    json,
                    new TypeReference<List<String>>() {
                    }
            );

        } catch (JsonProcessingException e) {
            return List.of();
        }
    }

    private LocalDateTime parseDeadline(String deadline) {
        if (deadline == null || deadline.isBlank()) {
            return null;
        }

        String text = deadline.trim();

        if ("null".equalsIgnoreCase(text)
                || "not specified".equalsIgnoreCase(text)
                || "not found".equalsIgnoreCase(text)
                || "n/a".equalsIgnoreCase(text)) {
            return null;
        }

        try {
            return LocalDateTime.parse(text);

        } catch (Exception ignored) {
        }

        try {
            return LocalDate.parse(text).atStartOfDay();

        } catch (Exception ignored) {
        }

        return null;
    }
}