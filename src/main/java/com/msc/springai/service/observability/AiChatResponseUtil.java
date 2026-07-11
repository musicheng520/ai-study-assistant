package com.msc.springai.service.observability;

import org.springframework.ai.chat.model.ChatResponse;

public final class AiChatResponseUtil {

    private AiChatResponseUtil() {
    }

    public static String extractText(
            ChatResponse response
    ) {
        if (response == null
                || response.getResult() == null
                || response.getResult().getOutput() == null) {
            return null;
        }

        return response
                .getResult()
                .getOutput()
                .getText();
    }
}