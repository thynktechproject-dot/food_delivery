package com.food_delivery.backend.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class PaymentResponse {
    private Long orderId;
    private double amount;
    private String status;
    private String transactionId;
    private LocalDateTime createdAt;
}
