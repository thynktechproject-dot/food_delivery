package com.food_delivery.backend.controller;

import com.food_delivery.backend.dto.ApiResponse;
import com.food_delivery.backend.dto.DeliveryStatsResponse;
import com.food_delivery.backend.dto.OrderResponse;
import com.food_delivery.backend.dto.UpdateAvailabilityRequest;
import com.food_delivery.backend.dto.UserResponse;
import com.food_delivery.backend.entity.OrderStatus;
import com.food_delivery.backend.service.OrderService;
import com.food_delivery.backend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/delivery")
@RequiredArgsConstructor
@PreAuthorize("hasRole('DELIVERY_AGENT')")
@Slf4j
public class DeliveryController {

    private final OrderService orderService;
    private final UserService userService;

    // -------------------------------------------------------------------------
    // Existing endpoints
    // -------------------------------------------------------------------------

    @GetMapping("/orders")
    public ApiResponse<Page<OrderResponse>> getAssignedOrders(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        log.info("GET /api/delivery/orders called by user: {}", authentication.getName());

        Long agentId = resolveCurrentUserId(authentication);
        log.debug("Fetching assigned orders for agentId: {}, page: {}, size: {}", agentId, page, size);

        return ApiResponse.<Page<OrderResponse>>builder()
                .success(true)
                .message("Assigned orders fetched")
                .data(orderService.getOrdersByDeliveryAgentId(agentId, page, size))
                .build();
    }

    @GetMapping("/orders/{orderId}")
    public ApiResponse<OrderResponse> getOrder(
            @PathVariable Long orderId,
            Authentication authentication
    ) {
        log.info("GET /api/delivery/orders/{} called by user: {}", orderId, authentication.getName());

        Long agentId = resolveCurrentUserId(authentication);
        log.debug("Fetching orderId: {} for agentId: {}", orderId, agentId);

        return ApiResponse.<OrderResponse>builder()
                .success(true)
                .message("Order fetched")
                .data(orderService.getOrderByDeliveryAgent(orderId, agentId))
                .build();
    }

    @PutMapping("/orders/{orderId}/status")
    public ApiResponse<OrderResponse> updateDeliveryStatus(
            @PathVariable Long orderId,
            @RequestParam OrderStatus status,
            Authentication authentication
    ) {
        log.info("PUT /api/delivery/orders/{}/status called by user: {}", orderId, authentication.getName());
        log.debug("Updating status for orderId: {} to {}", orderId, status);

        return ApiResponse.<OrderResponse>builder()
                .success(true)
                .message("Delivery status updated")
                .data(orderService.updateStatus(orderId, status, authentication.getName()))
                .build();
    }

    // -------------------------------------------------------------------------
    // New endpoints
    // -------------------------------------------------------------------------

    @GetMapping("/orders/active")
    public ApiResponse<Page<OrderResponse>> getActiveOrders(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        log.info("GET /api/delivery/orders/active called by user: {}", authentication.getName());

        Long agentId = resolveCurrentUserId(authentication);
        log.debug("Fetching active orders for agentId: {}, page: {}, size: {}", agentId, page, size);

        return ApiResponse.<Page<OrderResponse>>builder()
                .success(true)
                .message("Active orders fetched")
                .data(orderService.getActiveOrdersByDeliveryAgentId(agentId, page, size))
                .build();
    }

    @GetMapping("/orders/history")
    public ApiResponse<Page<OrderResponse>> getDeliveryHistory(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        log.info("GET /api/delivery/orders/history called by user: {}", authentication.getName());

        Long agentId = resolveCurrentUserId(authentication);
        log.debug("Fetching delivery history for agentId: {}, page: {}, size: {}", agentId, page, size);

        return ApiResponse.<Page<OrderResponse>>builder()
                .success(true)
                .message("Delivery history fetched")
                .data(orderService.getDeliveryHistoryByAgentId(agentId, page, size))
                .build();
    }

    @GetMapping("/stats")
    public ApiResponse<DeliveryStatsResponse> getStats(Authentication authentication) {
        log.info("GET /api/delivery/stats called by user: {}", authentication.getName());

        Long agentId = resolveCurrentUserId(authentication);
        log.debug("Fetching stats for agentId: {}", agentId);

        return ApiResponse.<DeliveryStatsResponse>builder()
                .success(true)
                .message("Delivery stats fetched")
                .data(orderService.getDeliveryStatsByAgentId(agentId))
                .build();
    }

    @PutMapping("/availability")
    public ApiResponse<UserResponse> updateAvailability(
            @Valid @RequestBody UpdateAvailabilityRequest request,
            Authentication authentication
    ) {
        log.info("PUT /api/delivery/availability called by user: {}", authentication.getName());
        log.debug("Updating availability to: {}", request.getAvailable());

        Long agentId = resolveCurrentUserId(authentication);

        if (Boolean.TRUE.equals(request.getAvailable())) {
            log.debug("Activating agentId: {}", agentId);
            userService.activateAgent(agentId);
        } else {
            log.debug("Deactivating agentId: {}", agentId);
            userService.deactivateAgent(agentId);
        }

        UserResponse updated = userService.getUserById(agentId);

        return ApiResponse.<UserResponse>builder()
                .success(true)
                .message("Availability updated")
                .data(updated)
                .build();
    }

    private Long resolveCurrentUserId(Authentication authentication) {
        log.debug("Resolving userId for email: {}", authentication.getName());
        return userService.getUserByEmail(authentication.getName()).getId();
    }
}