package com.msc.springai.dto.learning.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CourseWeakTopicsResponse {

    private Long courseId;

    private Integer topicCount;

    private List<WeakTopicResponse> weakTopics;
}