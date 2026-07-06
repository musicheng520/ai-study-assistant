package com.msc.springai.service;

import com.msc.springai.entity.CourseDocument;
import com.msc.springai.entity.DocumentProcessingJob;
import com.msc.springai.mapper.CourseDocumentMapper;
import com.msc.springai.mapper.DocumentProcessingJobMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;

@Service
@RequiredArgsConstructor
public class DocumentProcessingService {

    private final CourseDocumentMapper courseDocumentMapper;
    private final DocumentProcessingJobMapper documentProcessingJobMapper;

    @Async("documentProcessingExecutor")
    public void processDocumentAsync(Long jobId, Long documentId) {
        try {
            DocumentProcessingJob job = documentProcessingJobMapper.findById(jobId);
            if (job == null) {
                return;
            }

            CourseDocument document = courseDocumentMapper.findById(documentId);
            if (document == null) {
                documentProcessingJobMapper.markFailed(jobId, "Document not found");
                return;
            }

            documentProcessingJobMapper.markRunning(jobId, "PARSE");

            Path filePath = Path.of(document.getStoredFilePath());
            if (!Files.exists(filePath)) {
                throw new IllegalArgumentException("Stored file does not exist: " + document.getStoredFilePath());
            }

            simulateStep(jobId, "PARSE");
            simulateStep(jobId, "CLEAN");
            simulateStep(jobId, "CHUNK");

            courseDocumentMapper.updateProcessingResult(
                    documentId,
                    "READY",
                    null,
                    0
            );

            documentProcessingJobMapper.markSuccess(jobId);

        } catch (Exception ex) {
            courseDocumentMapper.updateProcessingResult(
                    documentId,
                    "FAILED",
                    ex.getMessage(),
                    0
            );

            documentProcessingJobMapper.markFailed(jobId, ex.getMessage());
        }
    }

    private void simulateStep(Long jobId, String step) throws InterruptedException {
        documentProcessingJobMapper.updateStep(jobId, step);
        Thread.sleep(500);
    }
}