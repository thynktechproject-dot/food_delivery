package com.food_delivery.backend.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CartResponse {
    private Long userId;
    private Long restaurantId;
    private List<CartItemResponse> items;
    private double totalAmount;
}