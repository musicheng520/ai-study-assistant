package com.msc.springai.service;

import com.msc.springai.entity.WorkflowType;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class WorkflowIntentRouter {

    public WorkflowType route(String message) {
        if (message == null || message.isBlank()) {
            return WorkflowType.UNKNOWN;
        }

        String text = message.toLowerCase(Locale.ROOT);

        if (containsAny(text,
                "checklist",
                "task list",
                "todo",
                "to-do",
                "tasks",
                "what should i finish"
        )) {
            return WorkflowType.CHECKLIST;
        }

        if (containsAny(text,
                "assignment",
                "brief",
                "deliverable",
                "deliverables",
                "requirement",
                "requirements",
                "submission",
                "deadline"
        )) {
            return WorkflowType.ASSIGNMENT_ANALYSIS;
        }

        if (containsAny(text,
                "rubric",
                "marking",
                "criteria",
                "criterion",
                "high mark",
                "high marks",
                "excellent",
                "grade",
                "grading"
        )) {
            return WorkflowType.RUBRIC_ANALYSIS;
        }

        if (containsAny(text,
                "review",
                "revise",
                "revision",
                "weak topic",
                "weak topics",
                "study next",
                "what should i study",
                "improve my weak",
                "wrong answers"
        )) {
            return WorkflowType.REVISION_PLAN;
        }

        if (containsAny(text,
                "quiz",
                "questions",
                "test me",
                "practice test",
                "mcq"
        )) {
            return WorkflowType.QUIZ;
        }

        if (containsAny(text,
                "flashcard",
                "flashcards",
                "card",
                "cards"
        )) {
            return WorkflowType.FLASHCARD;
        }

        if (containsAny(text,
                "summary",
                "summarize",
                "summarise",
                "revision notes",
                "key concepts"
        )) {
            return WorkflowType.SUMMARY;
        }

        if (containsAny(text,
                "explain",
                "what is",
                "what are",
                "why",
                "how",
                "tell me",
                "lecture",
                "document"
        )) {
            return WorkflowType.RAG_QA;
        }

        return WorkflowType.UNKNOWN;
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }

        return false;
    }
}