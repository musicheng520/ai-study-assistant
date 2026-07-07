package com.msc.springai.dev;

import com.msc.springai.service.EmbeddingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class DevEmbeddingController {

    private final EmbeddingService embeddingService;

    @GetMapping("/api/dev/embed-text")
    public Map<String, Object> embedText(@RequestParam String text) {
        System.out.println("[DevEmbeddingController] Start embedding test.");
        System.out.println("[DevEmbeddingController] Text = " + text);

        float[] embedding = embeddingService.embed(text);

        Map<String, Object> response = new HashMap<>();
        response.put("dimension", embedding.length);
        response.put("firstValues", Arrays.copyOfRange(
                embedding,
                0,
                Math.min(10, embedding.length)
        ));

        System.out.println("[DevEmbeddingController] Embedding test finished.");
        System.out.println("[DevEmbeddingController] Dimension = " + embedding.length);

        return response;
    }
}