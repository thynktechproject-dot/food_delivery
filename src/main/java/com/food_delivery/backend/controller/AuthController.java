package com.food_delivery.backend.controller;

import com.food_delivery.backend.dto.AuthResponse;
import com.food_delivery.backend.dto.CreateUserRequest;
import com.food_delivery.backend.dto.LoginRequest;
import com.food_delivery.backend.dto.RefreshTokenRequest;
import com.food_delivery.backend.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    @PostMapping("/register/restaurant-owner")
    public ResponseEntity<AuthResponse> registerRestaurantOwner(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.ok(authService.registerRestaurantOwner(request));
    }

    @PostMapping("/register/delivery-agent")
    public ResponseEntity<AuthResponse> registerDeliveryAgent(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.ok(authService.registerDeliveryAgent(request));
    }

    private final AuthService authService;

    @PostMapping("/register/user")
    public ResponseEntity<AuthResponse> registerUser(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.ok(authService.registerUser(request));
    }

    @PostMapping("/register/admin")
    public ResponseEntity<AuthResponse> registerAdmin(@Valid @RequestBody CreateUserRequest request) {
        // Allow public registration if no admins exist
        // Otherwise, require authentication as ADMIN
        if (authService.adminExists()) {
            // Only allow if current user is authenticated as ADMIN
            // This will throw if not authenticated or not admin
            org.springframework.security.core.Authentication authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated() || authentication.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
                throw new org.springframework.security.access.AccessDeniedException("Only admins can register new admins");
            }
        }
        return ResponseEntity.ok(authService.registerAdmin(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request);
        return ResponseEntity.noContent().build();
    }
}
