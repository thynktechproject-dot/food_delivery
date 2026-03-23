package com.food_delivery.backend.controller;

import com.food_delivery.backend.dto.ApiResponse;
import com.food_delivery.backend.dto.OrderResponse;
import com.food_delivery.backend.entity.OrderStatus;
import com.food_delivery.backend.service.OrderService;
import com.food_delivery.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/delivery")
@RequiredArgsConstructor
@PreAuthorize("hasRole('DELIVERY_AGENT')")
public class DeliveryController {

    private final OrderService orderService;
    private final UserService userService;

    @GetMapping("/orders")
    public ApiResponse<Page<OrderResponse>> getAssignedOrders(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Long deliveryAgentId = resolveCurrentUserId(authentication);
        return ApiResponse.<Page<OrderResponse>>builder()
                .success(true)
                .message("Assigned orders fetched")
                .data(orderService.getOrdersByDeliveryAgentId(deliveryAgentId, page, size))
                .build();
    }

    @GetMapping("/orders/{orderId}")
    public ApiResponse<OrderResponse> getOrder(@PathVariable Long orderId, Authentication authentication) {
        Long deliveryAgentId = resolveCurrentUserId(authentication);
        return ApiResponse.<OrderResponse>builder()
                .success(true)
                .message("Order fetched")
                .data(orderService.getOrderByDeliveryAgent(orderId, deliveryAgentId))
                .build();
    }

    @PutMapping("/orders/{orderId}/status")
    public ApiResponse<OrderResponse> updateDeliveryStatus(
            @PathVariable Long orderId,
            @RequestParam OrderStatus status,
            Authentication authentication
    ) {
        return ApiResponse.<OrderResponse>builder()
                .success(true)
                .message("Delivery status updated")
                .data(orderService.updateStatus(orderId, status, authentication.getName()))
                .build();
    }

    private Long resolveCurrentUserId(Authentication authentication) {
        return userService.getUserByEmail(authentication.getName()).getId();
    }
}
