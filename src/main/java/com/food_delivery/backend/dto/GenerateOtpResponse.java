package com.food_delivery.backend.dto;

import com.food_delivery.backend.entity.Role;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class GenerateOtpResponse {
    private String otp;
    private String message;
    private String identifier;
    private Role role;
    private LocalDateTime expiresAt;
}
