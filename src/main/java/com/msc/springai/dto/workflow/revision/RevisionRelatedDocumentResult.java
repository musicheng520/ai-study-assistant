package com.msc.springai.dto.workflow.revision;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RevisionRelatedDocumentResult {

    private Long documentId;

    private String fileName;
}