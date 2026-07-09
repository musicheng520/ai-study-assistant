package com.msc.springai.service;

import com.msc.springai.dto.learning.response.CourseWeakTopicsResponse;
import com.msc.springai.dto.learning.response.WeakTopicResponse;
import com.msc.springai.entity.Course;
import com.msc.springai.exception.BusinessException;
import com.msc.springai.mapper.CourseMapper;
import com.msc.springai.mapper.WrongAnswerMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProgressService {

    private final CourseMapper courseMapper;
    private final WrongAnswerMapper wrongAnswerMapper;

    public CourseWeakTopicsResponse getCourseWeakTopics(
            Long userId,
            Long courseId
    ) {
        validateCourseAccess(
                userId,
                courseId
        );

        List<WeakTopicResponse> weakTopics = wrongAnswerMapper.findWeakTopics(
                userId,
                courseId
        );

        return new CourseWeakTopicsResponse(
                courseId,
                weakTopics.size(),
                weakTopics
        );
    }

    private void validateCourseAccess(
            Long userId,
            Long courseId
    ) {
        Course course = courseMapper.findByIdAndUserId(
                courseId,
                userId
        );

        if (course == null) {
            throw new BusinessException(
                    "COURSE_ACCESS_DENIED",
                    "Course not found or access denied."
            );
        }
    }
}