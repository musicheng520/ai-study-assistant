package com.msc.springai.service.validator;

import com.msc.springai.dto.learning.result.*;
import com.msc.springai.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class AiOutputValidatorTest {

    private final SummaryOutputValidator summaryOutputValidator =
            new SummaryOutputValidator();

    private final QuizOutputValidator quizOutputValidator =
            new QuizOutputValidator();

    private final FlashcardOutputValidator flashcardOutputValidator =
            new FlashcardOutputValidator();

    @Test
    public void shouldValidateValidSummary() {
        SummaryResult result = buildValidSummary();

        assertDoesNotThrow(() ->
                summaryOutputValidator.validate(result)
        );
    }

    @Test
    public void shouldRejectInvalidSummary() {
        SummaryResult result = buildValidSummary();
        result.setSummary("Too short.");

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> summaryOutputValidator.validate(result)
        );

        assertEquals("AI_OUTPUT_INVALID", exception.getCode());
    }

    @Test
    public void shouldValidateValidQuiz() {
        QuizResult result = buildValidQuiz();

        assertDoesNotThrow(() ->
                quizOutputValidator.validate(result, 1, 1)
        );
    }

    @Test
    public void shouldRejectQuizWhenQuestionCountMismatch() {
        QuizResult result = buildValidQuiz();

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> quizOutputValidator.validate(result, 2, 1)
        );

        assertEquals("AI_OUTPUT_INVALID", exception.getCode());
    }

    @Test
    public void shouldRejectQuizWhenCorrectAnswerNotInOptions() {
        QuizResult result = buildValidQuiz();
        result.getQuestions().get(0).setCorrectAnswer("E. Unknown");

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> quizOutputValidator.validate(result, 1, 1)
        );

        assertEquals("AI_OUTPUT_INVALID", exception.getCode());
    }

    @Test
    public void shouldValidateValidFlashcards() {
        FlashcardResult result = buildValidFlashcards();

        assertDoesNotThrow(() ->
                flashcardOutputValidator.validate(result, 2)
        );
    }

    @Test
    public void shouldRejectFlashcardsWhenCountMismatch() {
        FlashcardResult result = buildValidFlashcards();

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> flashcardOutputValidator.validate(result, 3)
        );

        assertEquals("AI_OUTPUT_INVALID", exception.getCode());
    }

    @Test
    public void shouldRejectFlashcardWhenFrontEqualsBack() {
        FlashcardResult result = buildValidFlashcards();
        result.getCards().get(0).setBack(result.getCards().get(0).getFront());

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> flashcardOutputValidator.validate(result, 2)
        );

        assertEquals("AI_OUTPUT_INVALID", exception.getCode());
    }

    private SummaryResult buildValidSummary() {
        SummaryKeyConceptResult concept = new SummaryKeyConceptResult(
                "Retrieval",
                "Retrieval means finding relevant chunks from the uploaded course documents."
        );

        SummaryDefinitionResult definition = new SummaryDefinitionResult(
                "RAG",
                "Retrieval-Augmented Generation combines document retrieval with language generation."
        );

        SummaryResult result = new SummaryResult();

        result.setTitle("Introduction to RAG");
        result.setSummary(
                "Retrieval-Augmented Generation is a technique that first retrieves relevant document chunks " +
                        "from uploaded course materials and then uses those chunks to generate a grounded answer."
        );
        result.setKeyConcepts(List.of(concept));
        result.setDefinitions(List.of(definition));
        result.setRevisionNotes("Review retrieval, embeddings, chunks, citations and grounded generation.");
        result.setSourceScope("COURSE");

        return result;
    }

    private QuizResult buildValidQuiz() {
        QuizQuestionResult mcq = new QuizQuestionResult();

        mcq.setQuestionType("MCQ");
        mcq.setQuestionText("What does RAG retrieve before generation?");
        mcq.setOptions(List.of(
                "A. Relevant document chunks",
                "B. CSS files",
                "C. Database passwords",
                "D. Browser cookies"
        ));
        mcq.setCorrectAnswer("A. Relevant document chunks");
        mcq.setExplanation("RAG retrieves relevant chunks and uses them as context for generation.");
        mcq.setDifficulty("MEDIUM");
        mcq.setTopic("RAG");
        mcq.setSourceChunkId(101L);

        QuizQuestionResult shortAnswer = new QuizQuestionResult();

        shortAnswer.setQuestionType("SHORT_ANSWER");
        shortAnswer.setQuestionText("Explain why citations are useful in RAG.");
        shortAnswer.setOptions(List.of());
        shortAnswer.setCorrectAnswer("They show which document chunks support the answer.");
        shortAnswer.setExplanation("Citations make the generated answer traceable to the source material.");
        shortAnswer.setDifficulty("MEDIUM");
        shortAnswer.setTopic("Citation");
        shortAnswer.setSourceChunkId(102L);

        QuizResult result = new QuizResult();

        result.setTitle("RAG Quiz");
        result.setDifficulty("MEDIUM");
        result.setQuestions(List.of(mcq, shortAnswer));

        return result;
    }

    private FlashcardResult buildValidFlashcards() {
        FlashcardItemResult card1 = new FlashcardItemResult();

        card1.setFront("What is RAG?");
        card1.setBack("Retrieval-Augmented Generation.");
        card1.setTopic("RAG");
        card1.setDifficulty("EASY");
        card1.setSourceChunkId(101L);

        FlashcardItemResult card2 = new FlashcardItemResult();

        card2.setFront("Why are citations useful?");
        card2.setBack("They connect AI answers back to source chunks.");
        card2.setTopic("Citation");
        card2.setDifficulty("MEDIUM");
        card2.setSourceChunkId(102L);

        FlashcardResult result = new FlashcardResult();

        result.setTitle("RAG Flashcards");
        result.setCards(List.of(card1, card2));

        return result;
    }
}