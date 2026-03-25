package com.food_delivery.backend.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class RecentOrderResponse {

    private Long orderId;
    private Long userId;
    private List<String> items;
    private double totalAmount;
    private String status;
    private LocalDateTime createdAt;
}