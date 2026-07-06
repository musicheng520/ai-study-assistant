package com.msc.springai.controller;

import com.msc.springai.dto.document.DocumentResponse;
import com.msc.springai.dto.document.DocumentStatusResponse;
import com.msc.springai.entity.CourseDocument;
import com.msc.springai.security.CurrentUserUtil;
import com.msc.springai.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping(
            value = "/api/courses/{courseId}/documents",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public DocumentResponse uploadDocument(@PathVariable Long courseId,
                                           @RequestParam("file") MultipartFile file,
                                           @RequestParam(value = "documentType", defaultValue = "OTHER") String documentType) {
        Long currentUserId = CurrentUserUtil.getCurrentUserId();

        CourseDocument document = documentService.uploadDocument(
                currentUserId,
                courseId,
                file,
                documentType
        );

        return DocumentResponse.from(document);
    }

    @GetMapping("/api/courses/{courseId}/documents")
    public List<DocumentResponse> getCourseDocuments(@PathVariable Long courseId) {
        Long currentUserId = CurrentUserUtil.getCurrentUserId();

        return documentService.findCourseDocuments(currentUserId, courseId)
                .stream()
                .map(DocumentResponse::from)
                .toList();
    }

    @GetMapping("/api/documents/{documentId}")
    public DocumentResponse getDocument(@PathVariable Long documentId) {
        Long currentUserId = CurrentUserUtil.getCurrentUserId();

        CourseDocument document = documentService.findByIdAndCheckOwner(documentId, currentUserId);

        return DocumentResponse.from(document);
    }

    @GetMapping("/api/documents/{documentId}/status")
    public DocumentStatusResponse getDocumentStatus(@PathVariable Long documentId) {
        Long currentUserId = CurrentUserUtil.getCurrentUserId();

        CourseDocument document = documentService.findByIdAndCheckOwner(documentId, currentUserId);

        return new DocumentStatusResponse(
                document.getId(),
                document.getStatus(),
                document.getErrorMessage(),
                document.getChunkCount()
        );
    }

    @DeleteMapping("/api/documents/{documentId}")
    public ResponseEntity<Void> deleteDocument(@PathVariable Long documentId) {
        Long currentUserId = CurrentUserUtil.getCurrentUserId();

        documentService.deleteDocument(currentUserId, documentId);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/documents/{documentId}/retry")
    public DocumentResponse retryDocument(@PathVariable Long documentId) {
        Long currentUserId = CurrentUserUtil.getCurrentUserId();

        CourseDocument document = documentService.retryDocument(currentUserId, documentId);

        return DocumentResponse.from(document);
    }
}