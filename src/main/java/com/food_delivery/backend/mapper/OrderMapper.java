package com.food_delivery.backend.mapper;

import com.food_delivery.backend.dto.LiveOrderResponse;
import com.food_delivery.backend.dto.RecentOrderResponse;
import com.food_delivery.backend.entity.Order;

import java.util.stream.Collectors;

public class OrderMapper {

    // LIVE ORDER MAPPING
    public static LiveOrderResponse toLiveOrderResponse(Order order) {
        return LiveOrderResponse.builder()
                .orderId(order.getId())
                .userId(order.getUserId())
                .status(order.getOrderStatus().name())
                .createdAt(order.getCreatedAt())
                .build();
    }

    // RECENT ORDER MAPPING
    public static RecentOrderResponse toRecentOrderResponse(Order order) {
        return RecentOrderResponse.builder()
                .orderId(order.getId())
                .userId(order.getUserId())
                .items(order.getItems()
                        .stream()
                        .map(item -> item.getName()) // adjust if field differs
                        .collect(Collectors.toList()))
                .totalAmount(order.getTotalAmount())
                .status(order.getOrderStatus().name())
                .createdAt(order.getCreatedAt())
                .build();
    }
}