package com.msc.springai.dto.learning.projection;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DocumentReviewCandidate {

    private Long documentId;

    private String fileName;

    private LocalDateTime createdAt;
}