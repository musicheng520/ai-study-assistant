package com.msc.springai.dev;

import com.msc.springai.dto.document.ExtractedDocument;
import com.msc.springai.service.DocumentTextExtractor;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Paths;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class DevDocumentParseController {

    private final DocumentTextExtractor documentTextExtractor;

    @GetMapping("/api/dev/parse-document")
    public Map<String, Object> parseDocument(
            @RequestParam String path,
            @RequestParam String fileType
    ) {
        ExtractedDocument document = documentTextExtractor.extract(
                Paths.get(path),
                fileType
        );

        return Map.of(
                "totalPages", document.getTotalPages(),
                "pageCount", document.getPages().size(),
                "textLength", document.getFullText().length(),
                "preview", document.getFullText().substring(
                        0,
                        Math.min(500, document.getFullText().length())
                )
        );
    }
}