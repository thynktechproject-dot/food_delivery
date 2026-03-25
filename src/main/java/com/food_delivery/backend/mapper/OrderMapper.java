package com.food_delivery.backend.mapper;

import com.food_delivery.backend.dto.LiveOrderResponse;
import com.food_delivery.backend.dto.RecentOrderResponse;
import com.food_delivery.backend.entity.Order;
import com.food_delivery.backend.entity.Payment;

import java.util.List;

public class OrderMapper {

    
    public static LiveOrderResponse toLiveOrderResponse(Order order, String customerName) {
        return LiveOrderResponse.builder()
                .orderId(order.getId())
                .customerName(customerName)
                .orderStatus(order.getOrderStatus().name())
                .createdAt(order.getCreatedAt())
                .build();
    }

    
    public static RecentOrderResponse toRecentOrderResponse(
            Order order,
            Payment payment,
            String customerName
    ) {
        return RecentOrderResponse.builder()
                .orderId(order.getId())
                .customerName(customerName)
                .items(order.getItems()
                        .stream()
                        .map(item -> item.getName())
                        .toList())
                .totalAmount(order.getTotalAmount())
                .orderStatus(order.getOrderStatus().name())
                .paymentStatus(payment != null ? payment.getStatus().name() : "PENDING")
                .createdAt(order.getCreatedAt())
                .build();
    }
}