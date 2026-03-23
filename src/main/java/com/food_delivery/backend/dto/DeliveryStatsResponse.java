package com.food_delivery.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryStatsResponse {

    private long totalDelivered;
    private long activeDeliveries;
    private long totalAssigned;
}