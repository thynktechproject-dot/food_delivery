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

import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

	private final UserRepository userRepository;
	private final JwtUtil jwtUtil;
	private final PasswordEncoder passwordEncoder;
	private final AuthenticationManager authenticationManager;
	private final RefreshTokenRepository refreshTokenRepository;
	private final JwtProperties jwtProperties;

	@Override
	public AuthResponse registerRestaurantOwner(CreateUserRequest request) {
		log.info("Registering restaurant owner with email: {}", request.getEmail());
		return registerByRole(request, Role.RESTAURANT_OWNER);
	}

	@Override
	public AuthResponse registerDeliveryAgent(CreateUserRequest request) {
		log.info("Registering delivery agent with email: {}", request.getEmail());
		return registerByRole(request, Role.DELIVERY_AGENT);
	}

	@Override
	public boolean adminExists() {
		log.debug("Checking if admin exists");
		return userRepository.countByRoleAndDeletedAtIsNull(Role.ADMIN) > 0;
	}

	@Override
	public AuthResponse registerUser(CreateUserRequest request) {
		log.info("Registering user with email: {}", request.getEmail());
		return registerByRole(request, resolveRegisterUserRole(request));
	}

	@Override
	public AuthResponse registerAdmin(CreateUserRequest request) {
		log.info("Registering admin with email: {}", request.getEmail());
		return registerByRole(request, Role.ADMIN);
	}

	private Role resolveRegisterUserRole(CreateUserRequest request) {
		log.debug("Resolving role for registration request");

		if (request.getRole() == null) {
			return Role.USER;
		}

		if (request.getRole() != Role.USER) {
			log.warn("Invalid role attempted during public registration: {}", request.getRole());
			throw new BadRequestException("Public registration is only available for USER accounts");
		}

		return Role.USER;
	}

	private AuthResponse registerByRole(CreateUserRequest request, Role role) {

		log.debug("Registering user with email: {} and role: {}", request.getEmail(), role);

		if (userRepository.existsByEmailAndDeletedAtIsNull(request.getEmail())) {
			log.warn("Registration failed - email already exists: {}", request.getEmail());
			throw new BadRequestException("Email already exists");
		}

		User user = UserMapper.toEntity(request);
		user.setPassword(passwordEncoder.encode(request.getPassword()));
		user.setRole(role);

		User savedUser = userRepository.save(user);

		log.info("User registered successfully with id: {} and role: {}", savedUser.getId(), role);

		return buildAuthResponse(savedUser);
	}

	@Override
	public AuthResponse login(LoginRequest request) {
		log.info("Login attempt for email: {}", request.getEmail());

		try {
			authenticationManager
					.authenticate(new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
		} catch (DisabledException ex) {
			log.warn("Login failed - user account inactive: {}", request.getEmail());
			throw new ForbiddenException("User account is inactive");
		}

		User user = userRepository.findByEmailAndDeletedAtIsNull(request.getEmail())
				.orElseThrow(() -> {
					log.warn("Login failed - invalid credentials for email: {}", request.getEmail());
					return new BadRequestException("Invalid credentials");
				});

		Role role = user.getRole() != null ? user.getRole() : Role.USER;

		if (!user.isActive()) {
			log.warn("Login blocked - inactive user: {}", request.getEmail());
			throw new ForbiddenException("User account is inactive");
		}

		log.debug("Revoking old refresh tokens for userId: {}", user.getId());
		revokeUserRefreshTokens(user.getId());

		log.info("Login successful for userId: {} with role: {}", user.getId(), role);

		return buildAuthResponse(user);
	}

	@Override
	public AuthResponse refresh(RefreshTokenRequest request) {
		log.info("Refreshing token");

		RefreshToken refreshToken = refreshTokenRepository.findByTokenAndRevokedFalse(request.getRefreshToken())
				.orElseThrow(() -> {
					log.warn("Invalid refresh token used");
					return new BadRequestException("Refresh token is invalid");
				});

		if (refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
			log.warn("Expired refresh token for userId: {}", refreshToken.getUserId());
			revokeToken(refreshToken);
			throw new BadRequestException("Refresh token has expired");
		}

		User user = userRepository.findByIdAndDeletedAtIsNull(refreshToken.getUserId())
				.orElseThrow(() -> {
					log.warn("Invalid refresh token - user not found");
					return new BadRequestException("Refresh token is invalid");
				});

		if (!user.isActive()) {
			log.warn("Refresh blocked - inactive userId: {}", user.getId());
			revokeToken(refreshToken);
			throw new ForbiddenException("User account is inactive");
		}

		log.debug("Revoking used refresh token for userId: {}", user.getId());
		revokeToken(refreshToken);

		log.info("Token refreshed successfully for userId: {}", user.getId());

		return buildAuthResponse(user);
	}

	@Override
	public void logout(RefreshTokenRequest request) {
		log.info("Logout request received");

		refreshTokenRepository.findByTokenAndRevokedFalse(request.getRefreshToken()).ifPresent(token -> {
			log.debug("Revoking refresh token for userId: {}", token.getUserId());
			revokeToken(token);
		});
	}

	private AuthResponse buildAuthResponse(User user) {
		log.debug("Building auth response for userId: {}", user.getId());

		Role role = user.getRole() != null ? user.getRole() : Role.USER;
		String token = jwtUtil.generateToken(user.getEmail(), role.name(), user.getTokenVersion());
		String refreshToken = issueRefreshToken(user.getId());

		return AuthResponse.builder().token(token).refreshToken(refreshToken).tokenType("Bearer").role(role.name())
				.user(UserMapper.toResponse(user)).build();
	}

	private String issueRefreshToken(Long userId) {
		log.debug("Issuing refresh token for userId: {}", userId);

		RefreshToken refreshToken = RefreshToken.builder().userId(userId).token(UUID.randomUUID().toString())
				.expiresAt(LocalDateTime.now().plus(Duration.ofMillis(jwtProperties.getRefreshExpirationMillis())))
				.build();

		return refreshTokenRepository.save(refreshToken).getToken();
	}

	private void revokeUserRefreshTokens(Long userId) {
		log.debug("Revoking all active refresh tokens for userId: {}", userId);
		refreshTokenRepository.findByUserIdAndRevokedFalse(userId).forEach(this::revokeToken);
	}

	private void revokeToken(RefreshToken token) {
		log.debug("Revoking token for userId: {}", token.getUserId());

		token.setRevoked(true);
		token.setRevokedAt(LocalDateTime.now());
		refreshTokenRepository.save(token);
	}
}
