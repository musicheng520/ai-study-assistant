package com.msc.springai.service;

import com.msc.springai.dto.document.RedisChunkSearchResult;
import com.msc.springai.dto.rag.CitationResponse;
import com.msc.springai.dto.rag.RagAskRequest;
import com.msc.springai.dto.rag.RagAskResponse;
import com.msc.springai.entity.ChatMessage;
import com.msc.springai.entity.ChatMessageCitation;
import com.msc.springai.entity.ChatSession;
import com.msc.springai.entity.Course;
import com.msc.springai.entity.DocumentChunk;
import com.msc.springai.mapper.ChatMessageCitationMapper;
import com.msc.springai.mapper.ChatMessageMapper;
import com.msc.springai.mapper.ChatSessionMapper;
import com.msc.springai.mapper.CourseMapper;
import com.msc.springai.mapper.DocumentChunkMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class RagService {

    private static final String WORKFLOW_TYPE = "COURSE_RAG";
    private static final String NO_ANSWER_TEXT =
            "I do not have enough information in the uploaded course documents to answer this.";

    private final CourseMapper courseMapper;
    private final DocumentChunkMapper documentChunkMapper;

    private final ChatSessionMapper chatSessionMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final ChatMessageCitationMapper chatMessageCitationMapper;

    private final EmbeddingService embeddingService;
    private final RedisVectorService redisVectorService;
    private final RagPromptBuilder ragPromptBuilder;
    private final CitationBuilder citationBuilder;

    private final ChatClient.Builder chatClientBuilder;

    @Value("${spring.ai.openai.chat.options.model:unknown}")
    private String modelName;

    public RagAskResponse askCourse(
            Long userId,
            Long courseId,
            RagAskRequest request
    ) {
        System.out.println("[RagService] Start course RAG ask.");
        System.out.println("[RagService] userId = " + userId);
        System.out.println("[RagService] courseId = " + courseId);

        if (userId == null) {
            throw new RuntimeException("User id is required");
        }

        if (courseId == null) {
            throw new RuntimeException("Course id is required");
        }

        if (request == null) {
            throw new RuntimeException("Request body is required");
        }

        String question = request.getQuestion();

        if (question == null || question.isBlank()) {
            throw new RuntimeException("Question is required");
        }

        question = question.trim();

        int topK = normalizeTopK(request.getTopK());

        System.out.println("[RagService] question = " + question);
        System.out.println("[RagService] topK = " + topK);

        Course course = courseMapper.findByIdAndUserId(courseId, userId);

        if (course == null) {
            throw new RuntimeException("Course not found or access denied");
        }

        System.out.println("[RagService] Course verified.");
        System.out.println("[RagService] course name = " + course.getName());

        ChatSession session = getOrCreateSession(
                userId,
                courseId,
                request.getSessionId(),
                question
        );

        System.out.println("[RagService] sessionId = " + session.getId());

        ChatMessage userMessage = saveUserMessage(
                session.getId(),
                userId,
                courseId,
                question
        );

        System.out.println("[RagService] userMessageId = " + userMessage.getId());

        float[] questionEmbedding = embeddingService.embed(question);

        List<RedisChunkSearchResult> retrievedChunks =
                redisVectorService.searchCourseChunks(
                        userId,
                        courseId,
                        questionEmbedding,
                        topK
                );

        System.out.println("[RagService] Raw retrieved chunk count = "
                + retrievedChunks.size());

        List<RedisChunkSearchResult> validChunks = filterValidChunks(
                userId,
                courseId,
                retrievedChunks
        );

        System.out.println("[RagService] Valid retrieved chunk count = "
                + validChunks.size());

        if (validChunks.isEmpty()) {
            return buildNoAnswerResponse(
                    session.getId(),
                    userId,
                    courseId,
                    userMessage.getId(),
                    0
            );
        }

        String prompt = ragPromptBuilder.buildCourseRagPrompt(
                question,
                validChunks
        );

        String answer = callLlm(prompt);

        boolean noAnswer = isNoAnswer(answer);

        List<CitationResponse> citationResponses;

        if (noAnswer) {
            citationResponses = List.of();
        } else {
            citationResponses = citationBuilder.buildCitationResponses(validChunks);
        }

        ChatMessage assistantMessage = saveAssistantMessage(
                session.getId(),
                userId,
                courseId,
                answer,
                noAnswer
        );

        System.out.println("[RagService] assistantMessageId = "
                + assistantMessage.getId());

        if (!citationResponses.isEmpty()) {
            List<ChatMessageCitation> citationEntities =
                    citationBuilder.buildCitationEntities(
                            assistantMessage.getId(),
                            citationResponses
                    );

            int insertedCitationCount =
                    chatMessageCitationMapper.insertBatch(citationEntities);

            System.out.println("[RagService] Inserted citation count = "
                    + insertedCitationCount);
        }

        chatSessionMapper.touch(session.getId(), userId);

        System.out.println("[RagService] Course RAG ask finished.");
        System.out.println("[RagService] noAnswer = " + noAnswer);

        return new RagAskResponse(
                session.getId(),
                userMessage.getId(),
                assistantMessage.getId(),
                answer,
                noAnswer,
                WORKFLOW_TYPE,
                validChunks.size(),
                citationResponses
        );
    }

    private RagAskResponse buildNoAnswerResponse(
            Long sessionId,
            Long userId,
            Long courseId,
            Long userMessageId,
            Integer retrievedChunkCount
    ) {
        System.out.println("[RagService] Build no-answer response.");

        ChatMessage assistantMessage = saveAssistantMessage(
                sessionId,
                userId,
                courseId,
                NO_ANSWER_TEXT,
                true
        );

        chatSessionMapper.touch(sessionId, userId);

        return new RagAskResponse(
                sessionId,
                userMessageId,
                assistantMessage.getId(),
                NO_ANSWER_TEXT,
                true,
                WORKFLOW_TYPE,
                retrievedChunkCount,
                List.of()
        );
    }

    private ChatSession getOrCreateSession(
            Long userId,
            Long courseId,
            Long sessionId,
            String question
    ) {
        if (sessionId != null) {
            System.out.println("[RagService] Existing session requested.");
            System.out.println("[RagService] requested sessionId = " + sessionId);

            ChatSession existingSession =
                    chatSessionMapper.findByIdAndUserId(sessionId, userId);

            if (existingSession == null) {
                throw new RuntimeException("Chat session not found or access denied");
            }

            if (!Objects.equals(existingSession.getCourseId(), courseId)) {
                throw new RuntimeException("Chat session does not belong to this course");
            }

            return existingSession;
        }

        System.out.println("[RagService] Creating new chat session.");

        ChatSession session = new ChatSession();

        session.setUserId(userId);
        session.setCourseId(courseId);
        session.setTitle(buildSessionTitle(question));
        session.setScopeType("COURSE");
        session.setDocumentId(null);

        chatSessionMapper.insert(session);

        if (session.getId() == null) {
            throw new RuntimeException("Failed to create chat session");
        }

        System.out.println("[RagService] New session created.");
        System.out.println("[RagService] sessionId = " + session.getId());

        return session;
    }

    private ChatMessage saveUserMessage(
            Long sessionId,
            Long userId,
            Long courseId,
            String question
    ) {
        System.out.println("[RagService] Saving user message.");

        ChatMessage message = new ChatMessage();

        message.setSessionId(sessionId);
        message.setUserId(userId);
        message.setCourseId(courseId);
        message.setRole("USER");
        message.setContent(question);
        message.setWorkflowType(WORKFLOW_TYPE);
        message.setNoAnswer(false);
        message.setModelName(null);

        chatMessageMapper.insert(message);

        if (message.getId() == null) {
            throw new RuntimeException("Failed to save user message");
        }

        return message;
    }

    private ChatMessage saveAssistantMessage(
            Long sessionId,
            Long userId,
            Long courseId,
            String answer,
            boolean noAnswer
    ) {
        System.out.println("[RagService] Saving assistant message.");

        ChatMessage message = new ChatMessage();

        message.setSessionId(sessionId);
        message.setUserId(userId);
        message.setCourseId(courseId);
        message.setRole("ASSISTANT");
        message.setContent(answer);
        message.setWorkflowType(WORKFLOW_TYPE);
        message.setNoAnswer(noAnswer);
        message.setModelName(modelName);

        chatMessageMapper.insert(message);

        if (message.getId() == null) {
            throw new RuntimeException("Failed to save assistant message");
        }

        return message;
    }

    private String callLlm(String prompt) {
        System.out.println("[RagService] Start calling LLM.");
        System.out.println("[RagService] modelName = " + modelName);
        System.out.println("[RagService] prompt length = " + prompt.length());

        try {
            String answer = chatClientBuilder
                    .build()
                    .prompt()
                    .user(prompt)
                    .call()
                    .content();

            if (answer == null || answer.isBlank()) {
                throw new RuntimeException("LLM returned empty answer");
            }

            answer = answer.trim();

            System.out.println("[RagService] LLM answer generated.");
            System.out.println("[RagService] answer length = " + answer.length());

            return answer;

        } catch (Exception e) {
            System.out.println("[RagService] LLM call failed.");
            System.out.println("[RagService] error = " + e.getMessage());

            throw new RuntimeException(
                    "Failed to call LLM: " + e.getMessage(),
                    e
            );
        }
    }

    private List<RedisChunkSearchResult> filterValidChunks(
            Long userId,
            Long courseId,
            List<RedisChunkSearchResult> chunks
    ) {
        System.out.println("[RagService] Start filtering valid chunks.");

        List<RedisChunkSearchResult> validChunks = new ArrayList<>();

        if (chunks == null || chunks.isEmpty()) {
            return validChunks;
        }

        for (RedisChunkSearchResult chunk : chunks) {
            if (chunk == null) {
                continue;
            }

            if (chunk.getChunkId() == null) {
                System.out.println("[RagService] Skip chunk because chunkId is null.");
                continue;
            }

            if (!Objects.equals(chunk.getUserId(), userId)) {
                System.out.println("[RagService] Skip chunk because userId mismatch. chunkId = "
                        + chunk.getChunkId());
                continue;
            }

            if (!Objects.equals(chunk.getCourseId(), courseId)) {
                System.out.println("[RagService] Skip chunk because courseId mismatch. chunkId = "
                        + chunk.getChunkId());
                continue;
            }

            DocumentChunk dbChunk = documentChunkMapper.findByIdAndUserId(
                    chunk.getChunkId(),
                    userId
            );

            if (dbChunk == null) {
                System.out.println("[RagService] Skip chunk because it does not exist in MySQL. chunkId = "
                        + chunk.getChunkId());
                continue;
            }

            validChunks.add(chunk);
        }

        System.out.println("[RagService] Valid chunk count after filtering = "
                + validChunks.size());

        return validChunks;
    }

    private boolean isNoAnswer(String answer) {
        if (answer == null || answer.isBlank()) {
            return true;
        }

        String lower = answer.toLowerCase();

        return lower.contains("do not have enough information")
                || lower.contains("uploaded course documents do not provide enough information")
                || lower.contains("not enough information in the uploaded course documents");
    }

    private int normalizeTopK(Integer topK) {
        if (topK == null) {
            return 5;
        }

        if (topK < 1) {
            return 1;
        }

        return Math.min(topK, 10);
    }

    private String buildSessionTitle(String question) {
        if (question == null || question.isBlank()) {
            return "New chat";
        }

        String cleaned = question
                .replaceAll("\\s+", " ")
                .trim();

        if (cleaned.length() <= 80) {
            return cleaned;
        }

        return cleaned.substring(0, 80) + "...";
    }
}