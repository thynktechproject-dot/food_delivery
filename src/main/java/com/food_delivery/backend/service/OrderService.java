package com.food_delivery.backend.service;

import com.food_delivery.backend.dto.DeliveryStatsResponse;
import com.food_delivery.backend.dto.LiveOrderResponse;
import com.food_delivery.backend.dto.OrderResponse;
import com.food_delivery.backend.dto.RecentOrderResponse;
import com.food_delivery.backend.entity.OrderStatus;

import java.util.List;

import org.springframework.data.domain.Page;

public interface OrderService {

    OrderResponse placeOrder(Long userId);

    OrderResponse getOrderById(Long orderId);

    OrderResponse getOrderByUser(Long orderId, Long userId);

    OrderResponse getOrderByRestaurantOwner(Long orderId, Long ownerId);

    OrderResponse getOrderByDeliveryAgent(Long orderId, Long deliveryAgentId);

    Page<OrderResponse> getOrdersByUserId(Long userId, int page, int size);

    Page<OrderResponse> getOrdersByRestaurantOwner(Long ownerId, int page, int size);

    Page<OrderResponse> getOrdersByDeliveryAgentId(Long deliveryAgentId, int page, int size);

    /** Returns only orders currently in-progress for the given delivery agent. */
    Page<OrderResponse> getActiveOrdersByDeliveryAgentId(Long deliveryAgentId, int page, int size);

    /** Returns completed (DELIVERED) orders for the given delivery agent. */
    Page<OrderResponse> getDeliveryHistoryByAgentId(Long deliveryAgentId, int page, int size);

    /** Summary statistics for a delivery agent's own dashboard. */
    DeliveryStatsResponse getDeliveryStatsByAgentId(Long deliveryAgentId);

    OrderResponse updateStatus(Long orderId, OrderStatus newStatus, String actorEmail);

    Page<OrderResponse> getAllOrders(int page, int size);

    void cancelOrderByAdmin(Long orderId);

    OrderResponse assignDeliveryAgent(Long orderId, Long deliveryAgentId);
    
    List<LiveOrderResponse> getLiveOrders(Long restaurantId);

    List<RecentOrderResponse> getRecentOrders(Long restaurantId);
}