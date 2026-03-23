package com.food_delivery.backend.repository;

import com.food_delivery.backend.entity.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {

    Page<Restaurant> findByNameContainingIgnoreCase(String name, Pageable pageable);

    Page<Restaurant> findByApprovedTrueAndActiveTrue(Pageable pageable);

    Page<Restaurant> findByApprovedTrueAndActiveTrueAndNameContainingIgnoreCase(String name, Pageable pageable);

    Page<Restaurant> findByApprovedFalseAndActiveTrue(Pageable pageable);

    Optional<Restaurant> findByIdAndApprovedTrueAndActiveTrue(Long id);

    Optional<Restaurant> findByOwnerIdAndActiveTrue(Long ownerId);

    Optional<Restaurant> findByIdAndOwnerIdAndActiveTrue(Long id, Long ownerId);

    long countByApprovedAndActiveTrue(boolean approved);

    long countByActiveTrue();
}
