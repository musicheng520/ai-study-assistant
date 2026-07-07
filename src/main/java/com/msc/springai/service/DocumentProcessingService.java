package com.msc.springai.service;

import com.msc.springai.dto.document.ExtractedDocument;
import com.msc.springai.entity.CourseDocument;
import com.msc.springai.entity.DocumentChunk;
import com.msc.springai.entity.DocumentProcessingJob;
import com.msc.springai.mapper.CourseDocumentMapper;
import com.msc.springai.mapper.DocumentChunkMapper;
import com.msc.springai.mapper.DocumentProcessingJobMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DocumentProcessingService {

    private final CourseDocumentMapper courseDocumentMapper;
    private final DocumentProcessingJobMapper documentProcessingJobMapper;
    private final DocumentChunkMapper documentChunkMapper;

    private final DocumentTextExtractor documentTextExtractor;
    private final ChunkingService chunkingService;
    private final EmbeddingService embeddingService;
    private final RedisVectorService redisVectorService;
    private final RagCacheService ragCacheService;

    @Async("documentProcessingExecutor")
    public void processDocumentAsync(Long jobId, Long documentId) {
        Long userId = null;

        System.out.println("[DocumentProcessingService] Start real async document processing.");
        System.out.println("[DocumentProcessingService] jobId = " + jobId);
        System.out.println("[DocumentProcessingService] documentId = " + documentId);

        try {
            DocumentProcessingJob job = documentProcessingJobMapper.findById(jobId);

            if (job == null) {
                System.out.println("[DocumentProcessingService] Job not found. jobId = " + jobId);
                return;
            }

            userId = job.getUserId();
            Long courseId = job.getCourseId();

            System.out.println("[DocumentProcessingService] userId = " + userId);
            System.out.println("[DocumentProcessingService] courseId = " + courseId);

            CourseDocument document = courseDocumentMapper.findById(documentId);

            if (document == null) {
                System.out.println("[DocumentProcessingService] Document not found. documentId = " + documentId);

                documentProcessingJobMapper.markFailed(
                        jobId,
                        userId,
                        "Document not found"
                );

                return;
            }

            System.out.println("[DocumentProcessingService] Document found.");
            System.out.println("[DocumentProcessingService] originalFileName = " + document.getOriginalFileName());
            System.out.println("[DocumentProcessingService] storedFilePath = " + document.getStoredFilePath());
            System.out.println("[DocumentProcessingService] fileType = " + document.getFileType());
            System.out.println("[DocumentProcessingService] documentType = " + document.getDocumentType());

            Path filePath = Path.of(document.getStoredFilePath());

            if (!Files.exists(filePath)) {
                throw new IllegalArgumentException(
                        "Stored file does not exist: " + document.getStoredFilePath()
                );
            }

            if (!Files.isRegularFile(filePath)) {
                throw new IllegalArgumentException(
                        "Stored path is not a file: " + document.getStoredFilePath()
                );
            }

            documentProcessingJobMapper.markRunning(jobId, userId, "PARSE");

            System.out.println("[DocumentProcessingService] Step = PARSE");
            ExtractedDocument extractedDocument = documentTextExtractor.extract(
                    filePath,
                    document.getFileType()
            );

            System.out.println("[DocumentProcessingService] Extracted total pages = "
                    + extractedDocument.getTotalPages());
            System.out.println("[DocumentProcessingService] Extracted page count = "
                    + extractedDocument.getPages().size());
            System.out.println("[DocumentProcessingService] Extracted text length = "
                    + extractedDocument.getFullText().length());

            documentProcessingJobMapper.updateStep(jobId, userId, "CLEAN");

            System.out.println("[DocumentProcessingService] Step = CLEAN");
            System.out.println("[DocumentProcessingService] Text cleaning is handled inside ChunkingService.");

            documentProcessingJobMapper.updateStep(jobId, userId, "CHUNK");

            System.out.println("[DocumentProcessingService] Step = CHUNK");

            System.out.println("[DocumentProcessingService] Delete old vectors before rebuilding.");
            redisVectorService.deleteDocumentVectors(userId, documentId);

            System.out.println("[DocumentProcessingService] Delete old chunks before rebuilding.");
            int deletedChunks = documentChunkMapper.deleteByDocumentIdAndUserId(
                    documentId,
                    userId
            );
            System.out.println("[DocumentProcessingService] Deleted old chunk count = " + deletedChunks);

            List<DocumentChunk> chunks = chunkingService.buildChunks(
                    userId,
                    courseId,
                    documentId,
                    extractedDocument
            );

            System.out.println("[DocumentProcessingService] Generated chunk count = " + chunks.size());

            int insertedCount = documentChunkMapper.insertBatch(chunks);

            System.out.println("[DocumentProcessingService] Inserted chunk count = " + insertedCount);

            if (insertedCount <= 0) {
                throw new RuntimeException("No chunks were inserted into MySQL");
            }

            documentProcessingJobMapper.updateStep(jobId, userId, "EMBEDDING");

            System.out.println("[DocumentProcessingService] Step = EMBEDDING + VECTOR_INDEX");

            int successVectorCount = 0;

            for (DocumentChunk chunk : chunks) {
                if (chunk.getId() == null) {
                    throw new RuntimeException("Chunk id is null after insert. chunkIndex="
                            + chunk.getChunkIndex());
                }

                System.out.println("[DocumentProcessingService] Processing chunk vector.");
                System.out.println("[DocumentProcessingService] chunkId = " + chunk.getId());
                System.out.println("[DocumentProcessingService] chunkIndex = " + chunk.getChunkIndex());
                System.out.println("[DocumentProcessingService] pageNumber = " + chunk.getPageNumber());
                System.out.println("[DocumentProcessingService] content length = "
                        + chunk.getContent().length());

                float[] embedding = embeddingService.embed(chunk.getContent());

                documentProcessingJobMapper.updateStep(jobId, userId, "VECTOR_INDEX");

                String redisKey = redisVectorService.saveChunkVector(
                        chunk,
                        document.getOriginalFileName(),
                        document.getDocumentType(),
                        embedding
                );

                System.out.println("[DocumentProcessingService] Saved vector redisKey = " + redisKey);

                successVectorCount++;
            }

            System.out.println("[DocumentProcessingService] Success vector count = " + successVectorCount);

            courseDocumentMapper.updateProcessingResult(
                    documentId,
                    "READY",
                    null,
                    chunks.size()
            );

            documentProcessingJobMapper.markSuccess(jobId, userId);
            ragCacheService.evictCourseRagCache(userId, courseId);

            System.out.println("[DocumentProcessingService] Document processing success.");
            System.out.println("[DocumentProcessingService] documentId = " + documentId);
            System.out.println("[DocumentProcessingService] jobId = " + jobId);
            System.out.println("[DocumentProcessingService] final chunkCount = " + chunks.size());

        } catch (Exception ex) {
            String errorMessage = ex.getMessage();

            if (errorMessage == null || errorMessage.isBlank()) {
                errorMessage = "Document processing failed";
            }

            System.out.println("[DocumentProcessingService] Document processing failed.");
            System.out.println("[DocumentProcessingService] documentId = " + documentId);
            System.out.println("[DocumentProcessingService] jobId = " + jobId);
            System.out.println("[DocumentProcessingService] error = " + errorMessage);

            courseDocumentMapper.updateProcessingResult(
                    documentId,
                    "FAILED",
                    errorMessage,
                    0
            );

            if (userId != null) {
                documentProcessingJobMapper.markFailed(
                        jobId,
                        userId,
                        errorMessage
                );
            } else {
                System.out.println("[DocumentProcessingService] Cannot mark job failed because userId is null.");
            }
        }
    }
}