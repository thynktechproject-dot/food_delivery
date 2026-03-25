package com.food_delivery.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Sent by the frontend after the user completes the Razorpay checkout.
 * Razorpay gives these three values to the frontend's success callback.
 */
@Data
public class VerifyPaymentRequest {

    @NotNull(message = "orderId is required")
    private Long orderId;

    /** Razorpay order ID from Step 1 (e.g. order_AbCdEfGhIj1234) */
    @NotBlank(message = "razorpayOrderId is required")
    private String razorpayOrderId;

    /** Razorpay payment ID from the checkout callback (e.g. pay_AbCdEfGhIj1234) */
    @NotBlank(message = "razorpayPaymentId is required")
    private String razorpayPaymentId;

    /** HMAC-SHA256 signature from the checkout callback — used to verify authenticity */
    @NotBlank(message = "razorpaySignature is required")
    private String razorpaySignature;
}