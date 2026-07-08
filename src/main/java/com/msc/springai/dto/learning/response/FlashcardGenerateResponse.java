package com.msc.springai.dto.learning.response;

import com.msc.springai.dto.learning.result.FlashcardItemResult;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FlashcardGenerateResponse {

    private String draftKey;

    private String title;

    private String sourceScope;

    private Integer count;

    private String difficulty;

    private List<FlashcardItemResult> cards;
}