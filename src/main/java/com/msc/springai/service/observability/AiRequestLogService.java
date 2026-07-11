package com.msc.springai.service.observability;

import com.msc.springai.entity.AiRequestLog;
import com.msc.springai.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiRequestLogService {

    private static final int MAX_ERROR_MESSAGE_LENGTH = 2000;

    private final AiRequestLogWriter aiRequestLogWriter;

    @Value("${spring.ai.openai.chat.options.model:unknown}")
    private String configuredModelName;

    /**
     * 开始一次 AI 请求。
     */
    public AiRequestLogContext start(
            Long userId,
            Long courseId,
            String workflowType
    ) {
        return new AiRequestLogContext(
                userId,
                courseId,
                workflowType
        );
    }

    /**
     * 保存检索到的有效 chunk 数量。
     */
    public void setRetrievedChunkCount(
            AiRequestLogContext context,
            Integer retrievedChunkCount
    ) {
        if (context == null) {
            return;
        }

        context.setRetrievedChunkCount(
                retrievedChunkCount == null
                        ? 0
                        : Math.max(retrievedChunkCount, 0)
        );
    }

    /**
     * 从 Spring AI ChatResponse 中提取：
     *
     * model
     * promptTokens
     * completionTokens
     * totalTokens
     */
    public void captureResponseMetadata(
            AiRequestLogContext context,
            ChatResponse response
    ) {
        if (context == null || response == null) {
            return;
        }

        ChatResponseMetadata metadata =
                response.getMetadata();

        if (metadata == null) {
            useConfiguredModel(context);
            return;
        }

        String responseModel = metadata.getModel();

        if (responseModel == null || responseModel.isBlank()) {
            useConfiguredModel(context);
        } else {
            context.setModelName(responseModel.trim());
        }

        Usage usage = metadata.getUsage();

        if (usage == null) {
            return;
        }

        int promptTokens =
                valueOrZero(usage.getPromptTokens());

        int completionTokens =
                valueOrZero(usage.getCompletionTokens());

        Integer returnedTotalTokens =
                usage.getTotalTokens();

        int totalTokens = returnedTotalTokens == null
                ? promptTokens + completionTokens
                : Math.max(returnedTotalTokens, 0);

        context.setPromptTokens(promptTokens);
        context.setCompletionTokens(completionTokens);
        context.setTotalTokens(totalTokens);
    }

    /**
     * 正常模型调用成功。
     */
    public void completeSuccess(
            AiRequestLogContext context
    ) {
        persist(
                context,
                null,
                null
        );
    }

    /**
     * RAG 缓存命中。
     *
     * 没有真实调用模型，因此 tokens 全部为 0。
     */
    public void completeCacheHit(
            AiRequestLogContext context,
            Integer retrievedChunkCount
    ) {
        if (context == null) {
            return;
        }

        context.setModelName("RAG_CACHE");
        context.setPromptTokens(0);
        context.setCompletionTokens(0);
        context.setTotalTokens(0);
        context.setCacheHit(true);

        setRetrievedChunkCount(
                context,
                retrievedChunkCount
        );

        completeSuccess(context);
    }

    /**
     * 没有足够 chunks，直接返回 no-answer。
     *
     * 用户确实发起了一次 AI 请求，
     * 但后端没有把无证据问题发送给模型。
     */
    public void completeWithoutLlm(
            AiRequestLogContext context,
            Integer retrievedChunkCount
    ) {
        if (context == null) {
            return;
        }

        context.setModelName("NO_LLM");
        context.setPromptTokens(0);
        context.setCompletionTokens(0);
        context.setTotalTokens(0);
        context.setCacheHit(false);

        setRetrievedChunkCount(
                context,
                retrievedChunkCount
        );

        completeSuccess(context);
    }

    /**
     * AI 请求失败。
     */
    public void completeFailure(
            AiRequestLogContext context,
            Throwable throwable
    ) {
        String errorType =
                resolveErrorType(throwable);

        String errorMessage =
                resolveErrorMessage(throwable);

        persist(
                context,
                errorType,
                errorMessage
        );
    }

    private void persist(
            AiRequestLogContext context,
            String errorType,
            String errorMessage
    ) {
        if (context == null) {
            return;
        }

        /*
         * 同一个请求只能写入一次。
         */
        if (!context.tryComplete()) {
            return;
        }

        useConfiguredModel(context);

        long latencyMs = TimeUnit.NANOSECONDS.toMillis(
                System.nanoTime()
                        - context.getStartedNanos()
        );

        AiRequestLog requestLog =
                new AiRequestLog();

        requestLog.setUserId(context.getUserId());
        requestLog.setCourseId(context.getCourseId());

        requestLog.setWorkflowType(
                context.getWorkflowType()
        );

        requestLog.setModelName(
                context.getModelName()
        );

        requestLog.setPromptTokens(
                valueOrZero(context.getPromptTokens())
        );

        requestLog.setCompletionTokens(
                valueOrZero(context.getCompletionTokens())
        );

        requestLog.setTotalTokens(
                valueOrZero(context.getTotalTokens())
        );

        requestLog.setLatencyMs(
                Math.max(latencyMs, 0L)
        );

        requestLog.setCacheHit(
                Boolean.TRUE.equals(context.getCacheHit())
        );

        requestLog.setRetrievedChunkCount(
                valueOrZero(context.getRetrievedChunkCount())
        );

        requestLog.setErrorType(errorType);
        requestLog.setErrorMessage(errorMessage);
        requestLog.setCreatedAt(LocalDateTime.now());

        /*
         * Observability 代码不能导致正常业务失败。
         *
         * 即使日志数据库写入失败，
         * 主请求仍然应该继续返回。
         */
        try {
            aiRequestLogWriter.write(requestLog);

        } catch (Exception logException) {
            log.error(
                    "Failed to write AI request log. workflowType={}, error={}",
                    context.getWorkflowType(),
                    logException.getMessage()
            );
        }
    }

    private void useConfiguredModel(
            AiRequestLogContext context
    ) {
        if (context == null) {
            return;
        }

        String currentModel = context.getModelName();

        if (currentModel != null
                && !currentModel.isBlank()
                && !"unknown".equalsIgnoreCase(currentModel)) {
            return;
        }

        if (configuredModelName == null
                || configuredModelName.isBlank()) {
            context.setModelName("unknown");
            return;
        }

        context.setModelName(
                configuredModelName.trim()
        );
    }

    private String resolveErrorType(
            Throwable throwable
    ) {
        if (throwable == null) {
            return "UNKNOWN_AI_ERROR";
        }

        if (throwable instanceof BusinessException businessException) {
            String code = businessException.getCode();

            if (code != null && !code.isBlank()) {
                return truncate(
                        code.trim(),
                        100
                );
            }
        }

        return truncate(
                throwable.getClass().getSimpleName(),
                100
        );
    }

    private String resolveErrorMessage(
            Throwable throwable
    ) {
        if (throwable == null) {
            return "Unknown AI request failure.";
        }

        String message = throwable.getMessage();

        if (message == null || message.isBlank()) {
            message = throwable
                    .getClass()
                    .getSimpleName();
        }

        /*
         * 删除换行，防止数据库里保存整个巨大堆栈格式。
         */
        String cleaned = message
                .replaceAll("[\\r\\n\\t]+", " ")
                .replaceAll("\\s+", " ")
                .trim();

        return truncate(
                cleaned,
                MAX_ERROR_MESSAGE_LENGTH
        );
    }

    private int valueOrZero(
            Integer value
    ) {
        return value == null
                ? 0
                : Math.max(value, 0);
    }

    private String truncate(
            String value,
            int maxLength
    ) {
        if (value == null) {
            return null;
        }

        if (value.length() <= maxLength) {
            return value;
        }

        return value.substring(0, maxLength);
    }
}