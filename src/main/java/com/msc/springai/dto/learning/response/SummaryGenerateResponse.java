package com.msc.springai.dto.learning.response;

import com.msc.springai.dto.learning.result.SummaryDefinitionResult;
import com.msc.springai.dto.learning.result.SummaryKeyConceptResult;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SummaryGenerateResponse {

    private String draftKey;

    private String title;

    private String summary;

    private List<SummaryKeyConceptResult> keyConcepts;

    private List<SummaryDefinitionResult> definitions;

    private String revisionNotes;

    private String sourceScope;
}