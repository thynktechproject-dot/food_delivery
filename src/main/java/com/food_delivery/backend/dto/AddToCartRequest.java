package com.food_delivery.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AddToCartRequest {

    @NotNull
    private Long restaurantId;

    @NotNull
    private Long menuItemId;

    @Min(value = 1, message = "Quantity must be at least 1")
    private int quantity;
}
