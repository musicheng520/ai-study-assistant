package com.msc.springai.dto.learning.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RecentActivityResponse {

    private String eventType;

    private String targetType;

    private Long targetId;

    private String topic;

    private LocalDateTime createdAt;
}