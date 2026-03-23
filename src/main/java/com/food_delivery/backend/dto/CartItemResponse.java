package com.food_delivery.backend.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CartItemResponse {
    private Long menuItemId;
    private String name;
    private double price;
    private int quantity;
}