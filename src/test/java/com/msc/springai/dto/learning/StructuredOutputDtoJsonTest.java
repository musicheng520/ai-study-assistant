package com.msc.springai.dto.learning;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.msc.springai.dto.learning.draft.FlashcardDraftValue;
import com.msc.springai.dto.learning.draft.QuizDraftValue;
import com.msc.springai.dto.learning.draft.SummaryDraftValue;
import com.msc.springai.dto.learning.result.FlashcardResult;
import com.msc.springai.dto.learning.result.QuizResult;
import com.msc.springai.dto.learning.result.SummaryResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class StructuredOutputDtoJsonTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void shouldDeserializeSummaryResult() throws Exception {
        String json = """
                {
                  "title": "Introduction to RAG",
                  "summary": "RAG combines retrieval and generation.",
                  "keyConcepts": [
                    {
                      "name": "Retrieval",
                      "explanation": "Finds relevant chunks from documents."
                    }
                  ],
                  "definitions": [
                    {
                      "term": "RAG",
                      "definition": "Retrieval-Augmented Generation."
                    }
                  ],
                  "revisionNotes": "Review retrieval, embeddings and citations.",
                  "sourceScope": "COURSE"
                }
                """;

        SummaryResult result = objectMapper.readValue(json, SummaryResult.class);

        assertEquals("Introduction to RAG", result.getTitle());
        assertEquals("COURSE", result.getSourceScope());
        assertEquals(1, result.getKeyConcepts().size());
        assertEquals(1, result.getDefinitions().size());
        assertEquals("Retrieval", result.getKeyConcepts().get(0).getName());
        assertEquals("RAG", result.getDefinitions().get(0).getTerm());
    }

    @Test
    public void shouldDeserializeQuizResult() throws Exception {
        String json = """
                {
                  "title": "RAG Quiz",
                  "difficulty": "MEDIUM",
                  "questions": [
                    {
                      "questionType": "MCQ",
                      "questionText": "What does RAG use before generation?",
                      "options": [
                        "A. Retrieval",
                        "B. Compilation",
                        "C. Deployment",
                        "D. Styling"
                      ],
                      "correctAnswer": "A. Retrieval",
                      "explanation": "RAG retrieves relevant context before generation.",
                      "difficulty": "MEDIUM",
                      "topic": "RAG",
                      "sourceChunkId": 101
                    }
                  ]
                }
                """;

        QuizResult result = objectMapper.readValue(json, QuizResult.class);

        assertEquals("RAG Quiz", result.getTitle());
        assertEquals("MEDIUM", result.getDifficulty());
        assertEquals(1, result.getQuestions().size());
        assertEquals("MCQ", result.getQuestions().get(0).getQuestionType());
        assertEquals(4, result.getQuestions().get(0).getOptions().size());
        assertEquals("RAG", result.getQuestions().get(0).getTopic());
        assertEquals(101L, result.getQuestions().get(0).getSourceChunkId());
    }

    @Test
    public void shouldDeserializeFlashcardResult() throws Exception {
        String json = """
                {
                  "title": "RAG Flashcards",
                  "cards": [
                    {
                      "front": "What is RAG?",
                      "back": "Retrieval-Augmented Generation.",
                      "topic": "RAG",
                      "difficulty": "EASY",
                      "sourceChunkId": 102
                    }
                  ]
                }
                """;

        FlashcardResult result = objectMapper.readValue(json, FlashcardResult.class);

        assertEquals("RAG Flashcards", result.getTitle());
        assertEquals(1, result.getCards().size());
        assertEquals("What is RAG?", result.getCards().get(0).getFront());
        assertEquals("Retrieval-Augmented Generation.", result.getCards().get(0).getBack());
        assertEquals("EASY", result.getCards().get(0).getDifficulty());
        assertEquals(102L, result.getCards().get(0).getSourceChunkId());
    }

    @Test
    public void shouldSerializeAndDeserializeDraftValues() throws Exception {
        SummaryResult summaryResult = new SummaryResult();
        summaryResult.setTitle("Course Summary");
        summaryResult.setSummary("This is a course summary.");
        summaryResult.setRevisionNotes("Review the key ideas.");
        summaryResult.setSourceScope("COURSE");

        SummaryDraftValue summaryDraft = new SummaryDraftValue(
                1L,
                3L,
                null,
                "COURSE",
                summaryResult
        );

        String summaryJson = objectMapper.writeValueAsString(summaryDraft);
        SummaryDraftValue parsedSummaryDraft =
                objectMapper.readValue(summaryJson, SummaryDraftValue.class);

        assertEquals(1L, parsedSummaryDraft.getUserId());
        assertEquals(3L, parsedSummaryDraft.getCourseId());
        assertNull(parsedSummaryDraft.getDocumentId());
        assertEquals("COURSE", parsedSummaryDraft.getSourceScope());
        assertEquals("Course Summary", parsedSummaryDraft.getResult().getTitle());

        QuizResult quizResult = new QuizResult();
        quizResult.setTitle("Course Quiz");
        quizResult.setDifficulty("MEDIUM");

        QuizDraftValue quizDraft = new QuizDraftValue(
                1L,
                3L,
                10L,
                "DOCUMENT",
                5,
                3,
                "MEDIUM",
                quizResult
        );

        String quizJson = objectMapper.writeValueAsString(quizDraft);
        QuizDraftValue parsedQuizDraft =
                objectMapper.readValue(quizJson, QuizDraftValue.class);

        assertEquals(10L, parsedQuizDraft.getDocumentId());
        assertEquals("DOCUMENT", parsedQuizDraft.getSourceScope());
        assertEquals(5, parsedQuizDraft.getMcqCount());
        assertEquals(3, parsedQuizDraft.getShortAnswerCount());

        FlashcardResult flashcardResult = new FlashcardResult();
        flashcardResult.setTitle("Course Flashcards");

        FlashcardDraftValue flashcardDraft = new FlashcardDraftValue(
                1L,
                3L,
                null,
                "COURSE",
                10,
                "MEDIUM",
                flashcardResult
        );

        String flashcardJson = objectMapper.writeValueAsString(flashcardDraft);
        FlashcardDraftValue parsedFlashcardDraft =
                objectMapper.readValue(flashcardJson, FlashcardDraftValue.class);

        assertNull(parsedFlashcardDraft.getDocumentId());
        assertEquals("COURSE", parsedFlashcardDraft.getSourceScope());
        assertEquals(10, parsedFlashcardDraft.getCount());
        assertEquals("MEDIUM", parsedFlashcardDraft.getDifficulty());
    }
}