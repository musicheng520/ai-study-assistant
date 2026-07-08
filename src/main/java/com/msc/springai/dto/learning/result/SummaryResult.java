package com.msc.springai.dto.learning.result;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SummaryResult {

    private String title;

    private String summary;

    private List<SummaryKeyConceptResult> keyConcepts = new ArrayList<>();

    private List<SummaryDefinitionResult> definitions = new ArrayList<>();

    private String revisionNotes;

    private String sourceScope;
}