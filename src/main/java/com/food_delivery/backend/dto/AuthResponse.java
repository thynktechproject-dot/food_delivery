package com.food_delivery.backend.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {
    private String token;
    private String refreshToken;
    private String tokenType;
    private String role;
    private UserResponse user;
}
