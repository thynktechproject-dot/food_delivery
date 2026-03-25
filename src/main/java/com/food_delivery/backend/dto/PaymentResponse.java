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

    /** Razorpay order ID */
    private String razorpayOrderId;

    /** Razorpay payment ID — only present after successful verification */
    private String razorpayPaymentId;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}