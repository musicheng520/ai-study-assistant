package com.msc.springai.service;

import com.msc.springai.dto.document.RedisChunkSearchResult;
import com.msc.springai.dto.rag.CitationResponse;
import com.msc.springai.entity.ChatMessageCitation;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CitationBuilder {

    private static final int MAX_SNIPPET_CHARS = 500;

    public List<CitationResponse> buildCitationResponses(
            List<RedisChunkSearchResult> chunks
    ) {
        System.out.println("[CitationBuilder] Start building citation responses.");

        List<CitationResponse> citations = new ArrayList<>();

        if (chunks == null || chunks.isEmpty()) {
            System.out.println("[CitationBuilder] No chunks found.");
            return citations;
        }

        int citationIndex = 1;

        for (RedisChunkSearchResult chunk : chunks) {
            if (chunk == null) {
                continue;
            }

            CitationResponse citation = new CitationResponse();

            citation.setCitationIndex(citationIndex);
            citation.setDocumentId(chunk.getDocumentId());
            citation.setChunkId(chunk.getChunkId());
            citation.setFileName(chunk.getFileName());
            citation.setPageNumber(chunk.getPageNumber());
            citation.setSectionTitle(chunk.getSectionTitle());
            citation.setSnippet(buildSnippet(chunk.getContent()));
            citation.setDistance(chunk.getDistance());

            citations.add(citation);

            System.out.println("[CitationBuilder] Citation created."
                    + " index = " + citationIndex
                    + ", documentId = " + chunk.getDocumentId()
                    + ", chunkId = " + chunk.getChunkId()
                    + ", pageNumber = " + chunk.getPageNumber());

            citationIndex++;
        }

        System.out.println("[CitationBuilder] Citation response count = " + citations.size());

        return citations;
    }

    public List<ChatMessageCitation> buildCitationEntities(
            Long assistantMessageId,
            List<CitationResponse> citationResponses
    ) {
        System.out.println("[CitationBuilder] Start building citation entities.");
        System.out.println("[CitationBuilder] assistantMessageId = " + assistantMessageId);

        if (assistantMessageId == null) {
            throw new RuntimeException("Assistant message id is required");
        }

        List<ChatMessageCitation> citationEntities = new ArrayList<>();

        if (citationResponses == null || citationResponses.isEmpty()) {
            System.out.println("[CitationBuilder] No citation responses found.");
            return citationEntities;
        }

        for (CitationResponse response : citationResponses) {
            if (response == null) {
                continue;
            }

            ChatMessageCitation citation = new ChatMessageCitation();

            citation.setMessageId(assistantMessageId);
            citation.setDocumentId(response.getDocumentId());
            citation.setChunkId(response.getChunkId());
            citation.setFileName(response.getFileName());
            citation.setPageNumber(response.getPageNumber());
            citation.setSectionTitle(response.getSectionTitle());
            citation.setSnippet(response.getSnippet());

            citationEntities.add(citation);

            System.out.println("[CitationBuilder] Citation entity created."
                    + " messageId = " + assistantMessageId
                    + ", documentId = " + response.getDocumentId()
                    + ", chunkId = " + response.getChunkId());
        }

        System.out.println("[CitationBuilder] Citation entity count = "
                + citationEntities.size());

        return citationEntities;
    }

    private String buildSnippet(String content) {
        if (content == null || content.isBlank()) {
            return "";
        }

        String cleaned = content
                .replaceAll("\\s+", " ")
                .trim();

        if (cleaned.length() <= MAX_SNIPPET_CHARS) {
            return cleaned;
        }

        return cleaned.substring(0, MAX_SNIPPET_CHARS) + "...";
    }
}