package com.msc.springai.mapper;

import com.msc.springai.entity.StudyStreak;
import org.apache.ibatis.annotations.*;

import java.time.LocalDate;

@Mapper
public interface StudyStreakMapper {

    @Select("""
            SELECT *
            FROM study_streaks
            WHERE user_id = #{userId}
            """)
    StudyStreak findByUserId(@Param("userId") Long userId);

    @Insert("""
            INSERT INTO study_streaks (
                user_id,
                current_streak,
                longest_streak,
                last_activity_date,
                updated_at
            )
            VALUES (
                #{userId},
                #{currentStreak},
                #{longestStreak},
                #{lastActivityDate},
                NOW()
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(StudyStreak streak);

    @Update("""
            UPDATE study_streaks
            SET current_streak = #{currentStreak},
                longest_streak = #{longestStreak},
                last_activity_date = #{lastActivityDate},
                updated_at = NOW()
            WHERE user_id = #{userId}
            """)
    void update(StudyStreak streak);
}