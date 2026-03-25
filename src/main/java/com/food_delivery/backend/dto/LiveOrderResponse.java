package com.food_delivery.backend.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class LiveOrderResponse {

    private Long orderId;
    private String customerName;
    private String orderStatus;
    private LocalDateTime createdAt;
}