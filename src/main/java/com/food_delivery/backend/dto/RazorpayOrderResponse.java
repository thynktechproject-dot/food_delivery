package com.food_delivery.backend.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Returned to the frontend after Step 1 (creating a Razorpay order).
 * The frontend uses these fields to open the Razorpay checkout popup.
 */
@Data
@Builder
public class RazorpayOrderResponse {

    /** Your internal food delivery order ID */
    private Long orderId;

    /** Razorpay order ID — pass this to the Razorpay checkout */
    private String razorpayOrderId;

    /** Amount in paise (INR). E.g. ₹299.00 → 29900 */
    private long amountInPaise;

    /** Currency code, always "INR" */
    private String currency;

    /**
     * Your Razorpay Key ID (starts with rzp_test_ or rzp_live_).
     * The frontend needs this to initialize the Razorpay checkout SDK.
     */
    private String keyId;
}