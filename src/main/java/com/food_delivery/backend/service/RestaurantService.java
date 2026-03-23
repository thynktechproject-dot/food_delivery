package com.food_delivery.backend.service;

import com.food_delivery.backend.dto.*;
import org.springframework.data.domain.Page;

public interface RestaurantService {

    RestaurantResponse createRestaurant(CreateRestaurantRequest request, Long ownerId);

    RestaurantResponse getRestaurant(Long id, Long ownerId);

    RestaurantResponse updateRestaurant(Long id, CreateRestaurantRequest request, Long ownerId);

    void deleteRestaurant(Long id, Long ownerId);

    Page<RestaurantResponse> getAllRestaurants(int page, int size);

    Page<RestaurantResponse> getApprovedRestaurants(String keyword, int page, int size);

    RestaurantResponse getApprovedRestaurant(Long id);

    Page<RestaurantResponse> searchRestaurants(String keyword, int page, int size);

    Page<RestaurantResponse> getPendingRestaurants(int page, int size);

    void approveRestaurant(Long id);

    void rejectRestaurant(Long id);
}
