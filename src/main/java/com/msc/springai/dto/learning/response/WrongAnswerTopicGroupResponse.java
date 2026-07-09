package com.msc.springai.dto.learning.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WrongAnswerTopicGroupResponse {

    private String topic;

    private Integer count;

    private Integer unresolvedCount;

    private Integer resolvedCount;
}