package com.food_delivery.backend.controller;

import com.food_delivery.backend.dto.ApiResponse;
import com.food_delivery.backend.dto.ChangePasswordRequest;
import com.food_delivery.backend.dto.UpdateUserRequest;
import com.food_delivery.backend.dto.UserResponse;
import com.food_delivery.backend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user/profile")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('USER','RESTAURANT_OWNER','DELIVERY_AGENT')")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ApiResponse<UserResponse> me(Authentication authentication) {
        UserResponse user = userService.getUserByEmail(authentication.getName());

        return ApiResponse.<UserResponse>builder()
                .success(true)
                .message("Profile fetched")
                .data(user)
                .build();
    }

    @PutMapping("/me")
    public ApiResponse<UserResponse> updateProfile(
            @Valid @RequestBody UpdateUserRequest request,
            Authentication authentication
    ) {
        Long userId = resolveCurrentUserId(authentication);
        UserResponse updated = userService.updateUser(userId, request);

        return ApiResponse.<UserResponse>builder()
                .success(true)
                .message("Profile updated")
                .data(updated)
                .build();
    }

    @PutMapping("/me/password")
    public ApiResponse<String> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            Authentication authentication
    ) {
        Long userId = resolveCurrentUserId(authentication);
        userService.changePassword(userId, request);

        return ApiResponse.<String>builder()
                .success(true)
                .message("Password changed")
                .data(null)
                .build();
    }

    @DeleteMapping("/me")
    public ApiResponse<String> deleteProfile(Authentication authentication) {
        Long userId = resolveCurrentUserId(authentication);
        userService.deleteUser(userId);

        return ApiResponse.<String>builder()
                .success(true)
                .message("Profile deleted")
                .data(null)
                .build();
    }

    private Long resolveCurrentUserId(Authentication authentication) {
        return userService.getUserByEmail(authentication.getName()).getId();
    }
}
