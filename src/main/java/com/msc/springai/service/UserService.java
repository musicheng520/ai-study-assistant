package com.msc.springai.service;

import com.msc.springai.entity.User;
import com.msc.springai.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;

    public User createUserIfNotExists(String email, String displayName) {
        User existingUser = userMapper.findByEmail(email);
        if (existingUser != null) {
            return existingUser;
        }

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash("TEMP_PASSWORD_HASH");
        user.setDisplayName(displayName);
        user.setRole("USER");
        user.setStatus("ACTIVE");

        userMapper.insert(user);
        return user;
    }

    public User findById(Long id) {
        return userMapper.findById(id);
    }
}