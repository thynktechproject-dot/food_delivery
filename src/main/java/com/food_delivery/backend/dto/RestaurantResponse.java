package com.food_delivery.backend.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RestaurantResponse {
    private Long id;
    private Long ownerId;
    private String name;
    private String location;
    private String cuisineType;
    private boolean approved;
    private boolean active;
}
