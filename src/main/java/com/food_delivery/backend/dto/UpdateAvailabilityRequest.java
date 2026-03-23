package com.food_delivery.backend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateAvailabilityRequest {

    @NotNull
    private Boolean available;
}
