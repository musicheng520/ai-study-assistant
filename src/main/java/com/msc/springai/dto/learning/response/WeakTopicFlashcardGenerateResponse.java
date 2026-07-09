package com.msc.springai.dto.learning.response;

import com.msc.springai.dto.learning.result.FlashcardItemResult;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WeakTopicFlashcardGenerateResponse {

    private String draftKey;

    private String sourceType;

    private List<String> topics;

    private List<FlashcardItemResult> cards;
}