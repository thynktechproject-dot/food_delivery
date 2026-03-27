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

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/user/profile")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('USER','RESTAURANT_OWNER','DELIVERY_AGENT')")
@Slf4j
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ApiResponse<UserResponse> me(Authentication authentication) {
        log.info("GET /api/user/profile/me called by user: {}", authentication.getName());

        UserResponse user = userService.getUserByEmail(authentication.getName());

        log.debug("Fetched profile for user: {}", authentication.getName());

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
        log.info("PUT /api/user/profile/me called by user: {}", authentication.getName());

        Long userId = resolveCurrentUserId(authentication);
        log.debug("Updating profile for userId: {}", userId);

        UserResponse updated = userService.updateUser(userId, request);

        log.debug("Profile updated successfully for userId: {}", userId);

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
        log.info("PUT /api/user/profile/me/password called by user: {}", authentication.getName());

        Long userId = resolveCurrentUserId(authentication);
        log.debug("Changing password for userId: {}", userId);

        // ❌ Do NOT log password fields

        userService.changePassword(userId, request);

        log.debug("Password changed successfully for userId: {}", userId);

        return ApiResponse.<String>builder()
                .success(true)
                .message("Password changed")
                .data(null)
                .build();
    }

    @DeleteMapping("/me")
    public ApiResponse<String> deleteProfile(Authentication authentication) {
        log.info("DELETE /api/user/profile/me called by user: {}", authentication.getName());

        Long userId = resolveCurrentUserId(authentication);
        log.debug("Deleting profile for userId: {}", userId);

        userService.deleteUser(userId);

        log.debug("Profile deleted successfully for userId: {}", userId);

        return ApiResponse.<String>builder()
                .success(true)
                .message("Profile deleted")
                .data(null)
                .build();
    }

    private Long resolveCurrentUserId(Authentication authentication) {
        log.debug("Resolving userId for email: {}", authentication.getName());
        return userService.getUserByEmail(authentication.getName()).getId();
    }
}