package com.msc.springai.dto.learning.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SummaryGenerateRequest {

    private Integer topK;

    private String retrievalQuery;
}