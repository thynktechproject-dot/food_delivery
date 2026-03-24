package com.food_delivery.backend.service;

import com.food_delivery.backend.dto.*;

public interface AuthService {

	AuthResponse registerRestaurantOwner(CreateUserRequest request);

	AuthResponse registerDeliveryAgent(CreateUserRequest request);

	/**
	 * Returns true if at least one admin exists in the system.
	 */
	boolean adminExists();

	AuthResponse registerUser(CreateUserRequest request);

	AuthResponse registerAdmin(CreateUserRequest request);

	AuthResponse login(LoginRequest request);

	AuthResponse refresh(RefreshTokenRequest request);

	void logout(RefreshTokenRequest request);
}
