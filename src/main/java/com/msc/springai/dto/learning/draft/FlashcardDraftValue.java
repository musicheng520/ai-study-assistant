package com.msc.springai.dto.learning.draft;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.msc.springai.dto.learning.result.FlashcardResult;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FlashcardDraftValue {

    private Long userId;

    private Long courseId;

    private Long documentId;

    private String sourceScope;

    private Integer count;

    private String difficulty;

    private FlashcardResult result;
}