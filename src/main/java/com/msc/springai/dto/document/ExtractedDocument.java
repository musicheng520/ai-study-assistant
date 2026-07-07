package com.msc.springai.dto.document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ExtractedDocument {

    private String fullText;

    private Integer totalPages;

    private List<ExtractedPage> pages;
}