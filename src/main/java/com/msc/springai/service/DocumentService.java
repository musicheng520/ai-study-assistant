package com.msc.springai.service;

import com.msc.springai.entity.CourseDocument;
import com.msc.springai.entity.DocumentProcessingJob;
import com.msc.springai.mapper.CourseDocumentMapper;
import com.msc.springai.mapper.DocumentProcessingJobMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private static final long MAX_FILE_SIZE = 20L * 1024 * 1024;

    private final CourseService courseService;
    private final CourseDocumentMapper courseDocumentMapper;
    private final DocumentProcessingJobMapper documentProcessingJobMapper;

    @Value("${app.file.upload-dir}")
    private String uploadDir;

    public CourseDocument uploadDocument(Long userId,
                                         Long courseId,
                                         MultipartFile file,
                                         String documentType) {
        courseService.findByIdAndCheckOwner(courseId, userId);

        validateFile(file);

        try {
            String originalFileName = file.getOriginalFilename();
            String fileType = getFileType(originalFileName);

            String safeFileName = UUID.randomUUID() + "_" + originalFileName;

            Path uploadRoot = Path.of(uploadDir).toAbsolutePath().normalize();
            Path courseUploadDir = uploadRoot
                    .resolve(String.valueOf(userId))
                    .resolve(String.valueOf(courseId));

            Files.createDirectories(courseUploadDir);

            Path storedPath = courseUploadDir
                    .resolve(safeFileName)
                    .normalize();

            Files.copy(
                    file.getInputStream(),
                    storedPath,
                    StandardCopyOption.REPLACE_EXISTING
            );

            CourseDocument document = new CourseDocument();
            document.setUserId(userId);
            document.setCourseId(courseId);
            document.setOriginalFileName(originalFileName);
            document.setStoredFilePath(storedPath.toString());
            document.setFileType(fileType);
            document.setDocumentType(normalizeDocumentType(documentType));
            document.setFileSize(file.getSize());
            document.setStatus("PROCESSING");
            document.setVersion(1);
            document.setChunkCount(0);

            courseDocumentMapper.insert(document);

            DocumentProcessingJob job = new DocumentProcessingJob();
            job.setUserId(userId);
            job.setCourseId(courseId);
            job.setDocumentId(document.getId());
            job.setStatus("QUEUED");
            job.setStep("UPLOAD");
            job.setRetryCount(0);

            documentProcessingJobMapper.insert(job);

            return courseDocumentMapper.findById(document.getId());
        } catch (Exception ex) {
            throw new IllegalArgumentException("File upload failed: " + ex.getMessage());
        }
    }

    public List<CourseDocument> findCourseDocuments(Long userId, Long courseId) {
        courseService.findByIdAndCheckOwner(courseId, userId);
        return courseDocumentMapper.findByCourseIdAndUserId(courseId, userId);
    }

    public CourseDocument findByIdAndCheckOwner(Long documentId, Long userId) {
        CourseDocument document = courseDocumentMapper.findById(documentId);

        if (document == null) {
            throw new IllegalArgumentException("Document not found");
        }

        if (!document.getUserId().equals(userId)) {
            throw new IllegalArgumentException("You do not have permission to access this document");
        }

        return document;
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is required");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size must be less than 20MB");
        }

        String fileName = file.getOriginalFilename();

        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("File name is invalid");
        }

        String lowerName = fileName.toLowerCase();

        if (!lowerName.endsWith(".pdf") && !lowerName.endsWith(".docx")) {
            throw new IllegalArgumentException("Only PDF and DOCX files are allowed");
        }
    }

    private String getFileType(String fileName) {
        String lowerName = fileName.toLowerCase();

        if (lowerName.endsWith(".pdf")) {
            return "PDF";
        }

        if (lowerName.endsWith(".docx")) {
            return "DOCX";
        }

        throw new IllegalArgumentException("Unsupported file type");
    }

    private String normalizeDocumentType(String documentType) {
        if (documentType == null || documentType.isBlank()) {
            return "OTHER";
        }

        return documentType.trim().toUpperCase();
    }
}