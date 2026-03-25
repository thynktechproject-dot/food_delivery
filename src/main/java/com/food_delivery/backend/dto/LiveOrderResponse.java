package com.food_delivery.backend.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class LiveOrderResponse {

    private Long orderId;
    private Long userId;
    private String status;
    private LocalDateTime createdAt;
}