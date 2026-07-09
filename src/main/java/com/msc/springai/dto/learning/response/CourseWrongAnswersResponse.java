package com.msc.springai.dto.learning.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CourseWrongAnswersResponse {

    private Long courseId;

    private Boolean resolved;

    private String topic;

    private Integer total;

    private List<WrongAnswerTopicGroupResponse> topicGroups;

    private List<WrongAnswerItemResponse> items;
}