package com.food_delivery.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateRestaurantRequest {

    @NotBlank
    private String name;

    @NotBlank
    private String location;

    @NotBlank
    private String cuisineType;
}
