package com.food_delivery.backend.mapper;


import com.food_delivery.backend.dto.*;
import com.food_delivery.backend.entity.User;

public class UserMapper {

    public static User toEntity(CreateUserRequest request) {
        return User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(request.getPassword())
                .role(request.getRole())
                .phone(request.getPhone())
                .address(request.getAddress())
                .build();
    }

    public static UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole() != null ? user.getRole().name() : null)
                .active(user.isActive())
                .phone(user.getPhone())
                .address(user.getAddress())
                .build();
    }
}
