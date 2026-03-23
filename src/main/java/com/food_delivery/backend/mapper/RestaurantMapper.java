package com.food_delivery.backend.mapper;

import com.food_delivery.backend.dto.*;
import com.food_delivery.backend.entity.Restaurant;

public class RestaurantMapper {

    public static Restaurant toEntity(CreateRestaurantRequest request) {
        return Restaurant.builder()
                .name(request.getName())
                .location(request.getLocation())
                .cuisineType(request.getCuisineType())
                .build();
    }

    public static RestaurantResponse toResponse(Restaurant restaurant) {
        return RestaurantResponse.builder()
                .id(restaurant.getId())
                .ownerId(restaurant.getOwner() != null ? restaurant.getOwner().getId() : null)
                .name(restaurant.getName())
                .location(restaurant.getLocation())
                .cuisineType(restaurant.getCuisineType())
                .approved(restaurant.isApproved())
                .active(restaurant.isActive())
                .build();
    }
}
