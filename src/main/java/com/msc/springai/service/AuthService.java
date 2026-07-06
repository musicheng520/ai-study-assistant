package com.msc.springai.service;

import com.msc.springai.dto.auth.AuthResponse;
import com.msc.springai.dto.auth.LoginRequest;
import com.msc.springai.dto.auth.RegisterRequest;
import com.msc.springai.entity.User;
import com.msc.springai.mapper.UserMapper;
import com.msc.springai.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthResponse register(RegisterRequest request) {
        User existingUser = userMapper.findByEmail(request.getEmail());

        if (existingUser != null) {
            throw new IllegalArgumentException("Email is already registered");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setDisplayName(request.getDisplayName());
        user.setRole("USER");
        user.setStatus("ACTIVE");

        userMapper.insert(user);

        String token = jwtService.generateToken(user);
        return AuthResponse.from(user, token);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userMapper.findByEmail(request.getEmail());

        if (user == null) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        if (!"ACTIVE".equals(user.getStatus())) {
            throw new IllegalArgumentException("User account is not active");
        }

        String token = jwtService.generateToken(user);
        return AuthResponse.from(user, token);
    }

    public User findCurrentUser(Long userId) {
        User user = userMapper.findById(userId);

        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }

        return user;
    }
}