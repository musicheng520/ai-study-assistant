package com.msc.springai.controller;

import com.msc.springai.dto.auth.AuthResponse;
import com.msc.springai.dto.auth.CurrentUserResponse;
import com.msc.springai.dto.auth.LoginRequest;
import com.msc.springai.dto.auth.RegisterRequest;
import com.msc.springai.entity.User;
import com.msc.springai.security.CurrentUserUtil;
import com.msc.springai.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    public CurrentUserResponse getCurrentUser() {
        Long userId = CurrentUserUtil.getCurrentUserId();
        User user = authService.findCurrentUser(userId);
        return CurrentUserResponse.from(user);
    }
}