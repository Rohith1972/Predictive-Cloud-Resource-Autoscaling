package com.project.autoscaling.auth.service;

import com.project.autoscaling.auth.dto.AuthResponse;
import com.project.autoscaling.auth.dto.LoginRequest;
import com.project.autoscaling.auth.dto.RegisterRequest;
import com.project.autoscaling.auth.dto.UserProfileResponse;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    UserProfileResponse getCurrentUser(String email);
}
