package com.msc.springai.dto.workflow.revision;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RevisionPackResult {

    private String title;

    private String summary;

    private List<RevisionWeakTopicResult> weakTopics;

    private List<String> reviewOrder;

    private List<String> recommendedActions;

    private List<RevisionRelatedDocumentResult> relatedDocuments;

    private List<String> studyTasks;

    private List<String> suggestedFlashcards;
}