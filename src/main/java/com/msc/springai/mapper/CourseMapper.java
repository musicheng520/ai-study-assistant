package com.msc.springai.mapper;

import com.msc.springai.entity.Course;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface CourseMapper {

    @Insert("""
            INSERT INTO courses (user_id, name, code, description, color, progress_score)
            VALUES (#{userId}, #{name}, #{code}, #{description}, #{color}, #{progressScore})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Course course);

    @Select("""
            SELECT id, user_id, name, code, description, color, progress_score, created_at, updated_at
            FROM courses
            WHERE id = #{id}
            """)
    Course findById(Long id);

    @Select("""
            SELECT id, user_id, name, code, description, color, progress_score, created_at, updated_at
            FROM courses
            WHERE user_id = #{userId}
            ORDER BY created_at DESC
            """)
    List<Course> findByUserId(Long userId);

    @Select("""
            SELECT COUNT(*)
            FROM courses
            WHERE user_id = #{userId}
            """)
    int countByUserId(Long userId);
}