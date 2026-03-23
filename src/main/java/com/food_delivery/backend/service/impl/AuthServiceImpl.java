package com.food_delivery.backend.service.impl;

import com.food_delivery.backend.dto.*;
import com.food_delivery.backend.entity.RefreshToken;
import com.food_delivery.backend.entity.Role;
import com.food_delivery.backend.entity.User;
import com.food_delivery.backend.exception.BadRequestException;
import com.food_delivery.backend.exception.ForbiddenException;
import com.food_delivery.backend.mapper.UserMapper;
import com.food_delivery.backend.repository.RefreshTokenRepository;
import com.food_delivery.backend.repository.UserRepository;
import com.food_delivery.backend.service.AuthService;
import com.food_delivery.backend.config.properties.JwtProperties;
import com.food_delivery.backend.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProperties jwtProperties;

    @Override
    public AuthResponse registerUser(CreateUserRequest request) {
        return registerByRole(request, resolveRegisterUserRole(request));
    }


    @Override
    public AuthResponse registerAdmin(CreateUserRequest request) {
        return registerByRole(request, Role.ADMIN);
    }

    private Role resolveRegisterUserRole(CreateUserRequest request) {
        if (request.getRole() == null) {
            return Role.USER;
        }

        if (request.getRole() == Role.ADMIN) {
            throw new BadRequestException("Public registration is only available for USER, RESTAURANT_OWNER, or DELIVERY_AGENT accounts");
        }

        return request.getRole();
    }

    private AuthResponse registerByRole(CreateUserRequest request, Role role) {

        if (userRepository.existsByEmailAndDeletedAtIsNull(request.getEmail())) {
            throw new BadRequestException("Email already exists");
        }

        User user = UserMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(role);
        User savedUser = userRepository.save(user);
        return buildAuthResponse(savedUser);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (DisabledException ex) {
            throw new ForbiddenException("User account is inactive");
        }

        User user = userRepository.findByEmailAndDeletedAtIsNull(request.getEmail())
                .orElseThrow(() -> new BadRequestException("Invalid credentials"));

        Role role = user.getRole() != null ? user.getRole() : Role.USER;
        if (!user.isActive()) {
            throw new ForbiddenException("User account is inactive");
        }

        revokeUserRefreshTokens(user.getId());
        return buildAuthResponse(user);
    }

    @Override
    public AuthResponse refresh(RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenRepository.findByTokenAndRevokedFalse(request.getRefreshToken())
                .orElseThrow(() -> new BadRequestException("Refresh token is invalid"));

        if (refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            revokeToken(refreshToken);
            throw new BadRequestException("Refresh token has expired");
        }

        User user = userRepository.findByIdAndDeletedAtIsNull(refreshToken.getUserId())
                .orElseThrow(() -> new BadRequestException("Refresh token is invalid"));

        if (!user.isActive()) {
            revokeToken(refreshToken);
            throw new ForbiddenException("User account is inactive");
        }

        revokeToken(refreshToken);
        return buildAuthResponse(user);
    }

    @Override
    public void logout(RefreshTokenRequest request) {
        refreshTokenRepository.findByTokenAndRevokedFalse(request.getRefreshToken())
                .ifPresent(this::revokeToken);
    }

    private AuthResponse buildAuthResponse(User user) {
        Role role = user.getRole() != null ? user.getRole() : Role.USER;
        String token = jwtUtil.generateToken(user.getEmail(), user.getTokenVersion(), role.name());
        String refreshToken = issueRefreshToken(user.getId());

        return AuthResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .role(role.name())
                .user(UserMapper.toResponse(user))
                .build();
    }

    private String issueRefreshToken(Long userId) {
        RefreshToken refreshToken = RefreshToken.builder()
                .userId(userId)
                .token(UUID.randomUUID().toString())
                .expiresAt(LocalDateTime.now().plus(Duration.ofMillis(jwtProperties.getRefreshExpirationMillis())))
                .build();
        return refreshTokenRepository.save(refreshToken).getToken();
    }

    private void revokeUserRefreshTokens(Long userId) {
        refreshTokenRepository.findByUserIdAndRevokedFalse(userId)
                .forEach(this::revokeToken);
    }

    private void revokeToken(RefreshToken token) {
        token.setRevoked(true);
        token.setRevokedAt(LocalDateTime.now());
        refreshTokenRepository.save(token);
    }
}
