package com.msc.springai.dto.document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RedisChunkSearchResult {

    private String redisKey;

    private Long chunkId;

    private Long userId;

    private Long courseId;

    private Long documentId;

    private String fileName;

    private Integer pageNumber;

    private String sectionTitle;

    private String content;

    private Double distance;
}