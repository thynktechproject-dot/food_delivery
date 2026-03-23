package com.food_delivery.backend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateMenuItemRequest {

    @NotBlank
    private String name;

    @DecimalMin(value = "0.01", message = "Price must be greater than zero")
    private double price;
}
