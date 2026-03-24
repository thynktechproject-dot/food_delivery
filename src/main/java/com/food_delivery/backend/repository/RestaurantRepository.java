package com.food_delivery.backend.repository;

import com.food_delivery.backend.entity.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.food_delivery.backend.enums.RestaurantStatus;

import java.util.Optional;

public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {

	Page<Restaurant> findByNameContainingIgnoreCase(String name, Pageable pageable);

	Page<Restaurant> findByStatusAndActiveTrue(RestaurantStatus status, Pageable pageable);

	Page<Restaurant> findByStatusAndActiveTrueAndNameContainingIgnoreCase(RestaurantStatus status, String name,
			Pageable pageable);

	Optional<Restaurant> findByIdAndStatusAndActiveTrue(Long id, RestaurantStatus status);

	Optional<Restaurant> findByOwnerIdAndActiveTrue(Long ownerId);

	Optional<Restaurant> findByIdAndOwnerIdAndActiveTrue(Long id, Long ownerId);

	long countByStatusAndActiveTrue(RestaurantStatus status);

	long countByActiveTrue();
}
