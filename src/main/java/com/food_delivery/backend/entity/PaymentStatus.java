package com.food_delivery.backend.entity;

public enum PaymentStatus {
    /**
     * Razorpay order created, waiting for the user to complete checkout.
     * The payment record exists but money has not moved yet.
     */
    PENDING,

    /** Payment verified successfully — money received. */
    SUCCESS,

    /** Payment failed or was declined. */
    FAILED
}