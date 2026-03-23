package com.food_delivery.backend.service.impl;

import com.food_delivery.backend.dto.PaymentResponse;
import com.food_delivery.backend.entity.*;
import com.food_delivery.backend.exception.ConflictException;
import com.food_delivery.backend.exception.ForbiddenException;
import com.food_delivery.backend.exception.ResourceNotFoundException;
import com.food_delivery.backend.repository.*;
import com.food_delivery.backend.service.PaymentService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;

    @Override
    @Transactional
    public PaymentResponse processPayment(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (!order.getUserId().equals(userId)) {
            throw new ForbiddenException("You can only pay for your own orders");
        }

        if (order.getOrderStatus() == OrderStatus.PAID) {
            return paymentRepository.findByOrderId(orderId)
                    .map(this::toResponse)
                    .orElseThrow(() -> new ConflictException("Order is already marked paid but payment record is missing"));
        }

        if (order.getOrderStatus() != OrderStatus.CREATED) {
            throw new ConflictException("Only newly created orders can be paid");
        }

        if (paymentRepository.existsByOrderIdAndStatus(orderId, PaymentStatus.SUCCESS)) {
            return paymentRepository.findByOrderId(orderId)
                    .map(this::toResponse)
                    .orElseThrow(() -> new ConflictException("Payment has already been completed for this order"));
        }

        // Simulate success
        boolean success = true;

        Payment payment = Payment.builder()
                .orderId(orderId)
                .amount(order.getTotalAmount())
                .status(success ? PaymentStatus.SUCCESS : PaymentStatus.FAILED)
                .transactionId(UUID.randomUUID().toString())
                .build();

        Payment savedPayment = paymentRepository.save(payment);

        order.setOrderStatus(success ? OrderStatus.PAID : OrderStatus.FAILED);

        orderRepository.save(order);

        return toResponse(savedPayment);
    }

    @Override
    public PaymentResponse getPaymentByOrderId(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (!order.getUserId().equals(userId)) {
            throw new ForbiddenException("You can only view payments for your own orders");
        }

        Payment payment = paymentRepository.findTopByOrderIdOrderByIdDesc(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found for order"));

        return toResponse(payment);
    }

    private PaymentResponse toResponse(Payment payment) {
        return PaymentResponse.builder()
                .orderId(payment.getOrderId())
                .amount(payment.getAmount())
                .status(payment.getStatus().name())
                .transactionId(payment.getTransactionId())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}
