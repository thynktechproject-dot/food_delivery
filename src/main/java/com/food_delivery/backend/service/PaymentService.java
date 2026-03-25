package com.food_delivery.backend.service;

import com.food_delivery.backend.dto.PaymentResponse;
import com.food_delivery.backend.dto.RazorpayOrderResponse;
import com.food_delivery.backend.dto.VerifyPaymentRequest;

public interface PaymentService {

    /**
     * Step 1 — Create a Razorpay order for the given food delivery order.
     * Returns the Razorpay order ID and key needed by the frontend checkout.
     */
    RazorpayOrderResponse createRazorpayOrder(Long orderId, Long userId);

    /**
     * Step 2 — Verify the HMAC-SHA256 signature sent by Razorpay after checkout.
     * If valid, marks the payment SUCCESS and the order PAID.
     */
    PaymentResponse verifyAndCapturePayment(VerifyPaymentRequest request, Long userId);

    /**
     * Handle Razorpay webhook events (payment.captured, payment.failed).
     * The raw request body and the X-Razorpay-Signature header are passed in.
     */
    void handleWebhook(String payload, String razorpaySignature);

    /** Fetch payment details for an order — only accessible by the order's owner. */
    PaymentResponse getPaymentByOrderId(Long orderId, Long userId);
}