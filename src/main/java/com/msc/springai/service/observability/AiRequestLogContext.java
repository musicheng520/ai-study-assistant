package com.msc.springai.service.observability;

import lombok.Getter;
import lombok.Setter;

@Getter
public class AiRequestLogContext {

    private final Long userId;

    private final Long courseId;

    private final String workflowType;

    private final long startedNanos;

    @Setter
    private String modelName = "unknown";

    @Setter
    private Integer promptTokens = 0;

    @Setter
    private Integer completionTokens = 0;

    @Setter
    private Integer totalTokens = 0;

    @Setter
    private Boolean cacheHit = false;

    @Setter
    private Integer retrievedChunkCount = 0;

    /*
     * 防止同一个请求既记录 SUCCESS，
     * 又在外层 catch 中再次记录 FAILED。
     */
    private boolean completed;

    public AiRequestLogContext(
            Long userId,
            Long courseId,
            String workflowType
    ) {
        this.userId = userId;
        this.courseId = courseId;
        this.workflowType = workflowType;
        this.startedNanos = System.nanoTime();
    }

    public synchronized boolean tryComplete() {
        if (completed) {
            return false;
        }

        completed = true;
        return true;
    }
}