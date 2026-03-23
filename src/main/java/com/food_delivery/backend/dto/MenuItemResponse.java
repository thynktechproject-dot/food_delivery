package com.food_delivery.backend.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MenuItemResponse {
    private Long id;
    private Long restaurantId;
    private String name;
    private double price;
    private boolean available;
}
