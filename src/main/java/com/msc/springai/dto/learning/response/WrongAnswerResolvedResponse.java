package com.msc.springai.dto.learning.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WrongAnswerResolvedResponse {

    private Long wrongAnswerId;

    private Boolean resolved;

    private String topic;
}