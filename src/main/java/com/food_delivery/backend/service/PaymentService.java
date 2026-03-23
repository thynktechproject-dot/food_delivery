package com.food_delivery.backend.service;

import com.food_delivery.backend.dto.PaymentResponse;

public interface PaymentService {
    PaymentResponse processPayment(Long orderId, Long userId);

    PaymentResponse getPaymentByOrderId(Long orderId, Long userId);
}
