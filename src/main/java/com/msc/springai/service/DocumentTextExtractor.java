package com.msc.springai.service;

import com.msc.springai.dto.document.ExtractedDocument;
import com.msc.springai.dto.document.ExtractedPage;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class DocumentTextExtractor {

    public ExtractedDocument extract(Path filePath, String fileType) {
        if (filePath == null) {
            throw new RuntimeException("File path is required");
        }

        if (!Files.exists(filePath)) {
            throw new RuntimeException("Stored file does not exist: " + filePath);
        }

        if (fileType == null || fileType.isBlank()) {
            throw new RuntimeException("File type is required");
        }

        String normalizedFileType = fileType.trim().toUpperCase();

        try {
            return switch (normalizedFileType) {
                case "PDF" -> extractPdf(filePath);
                case "DOCX" -> extractDocx(filePath);
                default -> throw new RuntimeException("Unsupported file type: " + fileType);
            };
        } catch (IOException | TikaException | SAXException e) {
            log.error("Failed to extract document text. filePath={}, fileType={}",
                    filePath, fileType, e);
            throw new RuntimeException("Failed to extract document text: " + e.getMessage());
        }
    }

    private ExtractedDocument extractPdf(Path filePath) throws IOException {
        List<ExtractedPage> pages = new ArrayList<>();
        StringBuilder fullTextBuilder = new StringBuilder();

        try (PDDocument document = PDDocument.load(filePath.toFile())) {
            int totalPages = document.getNumberOfPages();

            PDFTextStripper stripper = new PDFTextStripper();

            for (int pageNumber = 1; pageNumber <= totalPages; pageNumber++) {
                stripper.setStartPage(pageNumber);
                stripper.setEndPage(pageNumber);

                String pageText = stripper.getText(document);
                pageText = normalizeExtractedText(pageText);

                if (!pageText.isBlank()) {
                    pages.add(new ExtractedPage(pageNumber, pageText));

                    fullTextBuilder
                            .append("\n\n")
                            .append("[Page ")
                            .append(pageNumber)
                            .append("]\n")
                            .append(pageText);
                }
            }

            String fullText = normalizeExtractedText(fullTextBuilder.toString());

            validateExtractedText(fullText);

            return new ExtractedDocument(fullText, totalPages, pages);
        }
    }

    private ExtractedDocument extractDocx(Path filePath)
            throws IOException, TikaException, SAXException {

        AutoDetectParser parser = new AutoDetectParser();
        BodyContentHandler handler = new BodyContentHandler(-1);
        Metadata metadata = new Metadata();
        ParseContext context = new ParseContext();

        try (InputStream inputStream = Files.newInputStream(filePath)) {
            parser.parse(inputStream, handler, metadata, context);
        }

        String fullText = normalizeExtractedText(handler.toString());

        validateExtractedText(fullText);

        List<ExtractedPage> pages = List.of(
                new ExtractedPage(null, fullText)
        );

        return new ExtractedDocument(fullText, 1, pages);
    }

    private String normalizeExtractedText(String text) {
        if (text == null) {
            return "";
        }

        return text
                .replace("\u0000", " ")
                .replaceAll("[\\t\\x0B\\f\\r]+", " ")
                .replaceAll(" +", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }

    private void validateExtractedText(String text) {
        if (text == null || text.isBlank()) {
            throw new RuntimeException("No readable text found in this document");
        }

        if (text.length() < 20) {
            throw new RuntimeException("Extracted text is too short");
        }
    }
}