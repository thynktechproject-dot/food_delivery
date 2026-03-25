package com.food_delivery.backend.controller;

import com.food_delivery.backend.dto.ApiResponse;
import com.food_delivery.backend.dto.LiveOrderResponse;
import com.food_delivery.backend.dto.OrderResponse;
import com.food_delivery.backend.dto.RecentOrderResponse;
import com.food_delivery.backend.entity.OrderStatus;
import com.food_delivery.backend.service.OrderService;
import com.food_delivery.backend.service.UserService;
import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final UserService userService;

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ApiResponse<OrderResponse> placeOrder(Authentication authentication) {
        Long userId = resolveCurrentUserId(authentication);
        return ApiResponse.<OrderResponse>builder()
                .success(true)
                .message("Order placed successfully")
                .data(orderService.placeOrder(userId))
                .build();
    }

    @GetMapping("/{orderId}")
    @PreAuthorize("hasRole('USER')")
    public ApiResponse<OrderResponse> getOrder(@PathVariable Long orderId, Authentication authentication) {
        Long userId = resolveCurrentUserId(authentication);
        return ApiResponse.<OrderResponse>builder()
                .success(true)
                .message("Order fetched successfully")
                .data(orderService.getOrderByUser(orderId, userId))
                .build();
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('USER')")
    public ApiResponse<Page<OrderResponse>> getOrdersByUser(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Long userId = resolveCurrentUserId(authentication);
        return ApiResponse.<Page<OrderResponse>>builder()
                .success(true)
                .message("Orders fetched successfully")
                .data(orderService.getOrdersByUserId(userId, page, size))
                .build();
    }

    @PutMapping("/{orderId}/status")
    @PreAuthorize("hasAnyRole('ADMIN','RESTAURANT_OWNER','DELIVERY_AGENT')")
    public ApiResponse<OrderResponse> updateStatus(
            @PathVariable Long orderId,
            @RequestParam OrderStatus status,
            Authentication authentication
    ) {
        return ApiResponse.<OrderResponse>builder()
                .success(true)
                .message("Order status updated")
                .data(orderService.updateStatus(orderId, status, authentication.getName()))
                .build();
    }

    private Long resolveCurrentUserId(Authentication authentication) {
        return userService.getUserByEmail(authentication.getName()).getId();
    }
    
 // LIVE ORDER QUEUE
    @GetMapping("/live")
    @PreAuthorize("hasAnyRole('ADMIN','RESTAURANT_OWNER')")
    public ApiResponse<List<LiveOrderResponse>> getLiveOrders(
            @RequestParam Long restaurantId
    ) {

        List<LiveOrderResponse> liveOrders =
                orderService.getLiveOrders(restaurantId);

        return ApiResponse.<List<LiveOrderResponse>>builder()
                .success(true)
                .message("Live orders fetched successfully")
                .data(liveOrders)
                .build();
    }

    // RECENT ORDERS
    @GetMapping("/recent")
    @PreAuthorize("hasAnyRole('ADMIN','RESTAURANT_OWNER')")
    public ApiResponse<List<RecentOrderResponse>> getRecentOrders(
            @RequestParam Long restaurantId
    ) {

        List<RecentOrderResponse> recentOrders =
                orderService.getRecentOrders(restaurantId);

        return ApiResponse.<List<RecentOrderResponse>>builder()
                .success(true)
                .message("Recent orders fetched successfully")
                .data(recentOrders)
                .build();
    }

}
