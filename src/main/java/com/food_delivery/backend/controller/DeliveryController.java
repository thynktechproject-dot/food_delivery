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

@RestController
@RequestMapping("/api/delivery")
@RequiredArgsConstructor
@PreAuthorize("hasRole('DELIVERY_AGENT')")
public class DeliveryController {

    private final OrderService orderService;
    private final UserService userService;

    // -------------------------------------------------------------------------
    // Existing endpoints
    // -------------------------------------------------------------------------

    /** All orders ever assigned to the calling delivery agent (paginated). */
    @GetMapping("/orders")
    public ApiResponse<Page<OrderResponse>> getAssignedOrders(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Long agentId = resolveCurrentUserId(authentication);
        return ApiResponse.<Page<OrderResponse>>builder()
                .success(true)
                .message("Assigned orders fetched")
                .data(orderService.getOrdersByDeliveryAgentId(agentId, page, size))
                .build();
    }

    /** Single order detail — only accessible if it is assigned to the calling agent. */
    @GetMapping("/orders/{orderId}")
    public ApiResponse<OrderResponse> getOrder(
            @PathVariable Long orderId,
            Authentication authentication
    ) {
        Long agentId = resolveCurrentUserId(authentication);
        return ApiResponse.<OrderResponse>builder()
                .success(true)
                .message("Order fetched")
                .data(orderService.getOrderByDeliveryAgent(orderId, agentId))
                .build();
    }

    /** Update the delivery status of an assigned order (PREPARING → OUT_FOR_DELIVERY → DELIVERED). */
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

    // -------------------------------------------------------------------------
    // New endpoints
    // -------------------------------------------------------------------------

    /**
     * Active orders only — those currently PREPARING or OUT_FOR_DELIVERY.
     * Intended for the agent's live "on-the-road" view.
     */
    @GetMapping("/orders/active")
    public ApiResponse<Page<OrderResponse>> getActiveOrders(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Long agentId = resolveCurrentUserId(authentication);
        return ApiResponse.<Page<OrderResponse>>builder()
                .success(true)
                .message("Active orders fetched")
                .data(orderService.getActiveOrdersByDeliveryAgentId(agentId, page, size))
                .build();
    }

    /**
     * Delivery history — orders with status DELIVERED, sorted newest-first.
     * Useful for the agent's earnings/history screen.
     */
    @GetMapping("/orders/history")
    public ApiResponse<Page<OrderResponse>> getDeliveryHistory(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Long agentId = resolveCurrentUserId(authentication);
        return ApiResponse.<Page<OrderResponse>>builder()
                .success(true)
                .message("Delivery history fetched")
                .data(orderService.getDeliveryHistoryByAgentId(agentId, page, size))
                .build();
    }

    /**
     * Personal stats dashboard: total assigned, currently active, total delivered.
     */
    @GetMapping("/stats")
    public ApiResponse<DeliveryStatsResponse> getStats(Authentication authentication) {
        Long agentId = resolveCurrentUserId(authentication);
        return ApiResponse.<DeliveryStatsResponse>builder()
                .success(true)
                .message("Delivery stats fetched")
                .data(orderService.getDeliveryStatsByAgentId(agentId))
                .build();
    }

    /**
     * Allows the delivery agent to mark themselves available or unavailable.
     * When unavailable, admins will not assign new orders to them.
     */
    @PutMapping("/availability")
    public ApiResponse<UserResponse> updateAvailability(
            @Valid @RequestBody UpdateAvailabilityRequest request,
            Authentication authentication
    ) {
        Long agentId = resolveCurrentUserId(authentication);
        if (Boolean.TRUE.equals(request.getAvailable())) {
            userService.activateAgent(agentId);
        } else {
            userService.deactivateAgent(agentId);
        }
        UserResponse updated = userService.getUserById(agentId);
        return ApiResponse.<UserResponse>builder()
                .success(true)
                .message("Availability updated")
                .data(updated)
                .build();
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private Long resolveCurrentUserId(Authentication authentication) {
        return userService.getUserByEmail(authentication.getName()).getId();
    }
}