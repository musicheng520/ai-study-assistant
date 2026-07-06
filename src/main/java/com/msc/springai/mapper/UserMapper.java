package com.msc.springai.mapper;

import com.msc.springai.entity.User;
import org.apache.ibatis.annotations.*;

@Mapper
public interface UserMapper {

    @Insert("""
            INSERT INTO users (email, password_hash, display_name, role, status)
            VALUES (#{email}, #{passwordHash}, #{displayName}, #{role}, #{status})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);

    @Select("""
            SELECT id, email, password_hash, display_name, role, status, created_at, updated_at
            FROM users
            WHERE id = #{id}
            """)
    User findById(Long id);

    @Select("""
            SELECT id, email, password_hash, display_name, role, status, created_at, updated_at
            FROM users
            WHERE email = #{email}
            """)
    User findByEmail(String email);
}