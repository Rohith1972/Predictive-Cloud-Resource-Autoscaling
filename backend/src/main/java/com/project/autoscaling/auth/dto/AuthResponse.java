package com.project.autoscaling.auth.dto;

import com.project.autoscaling.common.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String token;
    @Builder.Default
    private String tokenType = "Bearer";
    private long expiresIn;
    private Long userId;
    private String email;
    private String fullName;
    private UserRole role;
}
