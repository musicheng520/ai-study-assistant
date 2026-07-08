package com.msc.springai.dto.learning.draft;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.msc.springai.dto.learning.result.SummaryResult;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SummaryDraftValue {

    private Long userId;

    private Long courseId;

    private Long documentId;

    private String sourceScope;

    private SummaryResult result;
}