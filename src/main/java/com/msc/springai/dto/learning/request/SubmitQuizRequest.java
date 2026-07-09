package com.msc.springai.dto.learning.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SubmitQuizRequest {

    private List<AnswerItem> answers;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AnswerItem {

        private Long questionId;

        private String answer;
    }
}