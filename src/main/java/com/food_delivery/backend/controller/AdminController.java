package com.food_delivery.backend.controller;

import com.food_delivery.backend.dto.ApiResponse;
import com.food_delivery.backend.dto.CreateUserRequest;
import com.food_delivery.backend.dto.OrderResponse;
import com.food_delivery.backend.dto.RestaurantResponse;
import com.food_delivery.backend.dto.UserResponse;
import com.food_delivery.backend.service.OrderService;
import com.food_delivery.backend.service.RestaurantService;
import com.food_delivery.backend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserService userService;
    private final RestaurantService restaurantService;
    private final OrderService orderService;

    @PostMapping("/users")
    public ApiResponse<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        return ApiResponse.<UserResponse>builder()
                .success(true)
                .message("User created")
                .data(userService.createUser(request))
                .build();
    }

    @GetMapping("/users")
    public ApiResponse<Page<UserResponse>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.<Page<UserResponse>>builder()
                .success(true)
                .message("Users fetched")
                .data(userService.getAllUsers(page, size))
                .build();
    }

    @GetMapping("/users/{id}")
    public ApiResponse<UserResponse> getUser(@PathVariable Long id) {
        return ApiResponse.<UserResponse>builder()
                .success(true)
                .message("User fetched")
                .data(userService.getUserById(id))
                .build();
    }

    @PutMapping("/users/{id}/block")
    public ApiResponse<String> blockUser(@PathVariable Long id) {
        userService.blockUser(id);
        return ApiResponse.<String>builder()
                .success(true)
                .message("User blocked")
                .data(null)
                .build();
    }

    @PutMapping("/users/{id}/unblock")
    public ApiResponse<String> unblockUser(@PathVariable Long id) {
        userService.unblockUser(id);
        return ApiResponse.<String>builder()
                .success(true)
                .message("User unblocked")
                .data(null)
                .build();
    }

    @DeleteMapping("/users/{id}")
    public ApiResponse<String> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ApiResponse.<String>builder()
                .success(true)
                .message("User deleted")
                .data(null)
                .build();
    }

    @GetMapping("/restaurants")
    public ApiResponse<Page<RestaurantResponse>> getAllRestaurants(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.<Page<RestaurantResponse>>builder()
                .success(true)
                .message("Restaurants fetched")
                .data(restaurantService.getAllRestaurants(page, size))
                .build();
    }

    @GetMapping("/restaurants/pending")
    public ApiResponse<Page<RestaurantResponse>> getPendingRestaurants(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.<Page<RestaurantResponse>>builder()
                .success(true)
                .message("Pending restaurants fetched")
                .data(restaurantService.getPendingRestaurants(page, size))
                .build();
    }

    @PutMapping("/restaurants/{id}/approve")
    public ApiResponse<String> approveRestaurant(@PathVariable Long id) {
        restaurantService.approveRestaurant(id);
        return ApiResponse.<String>builder()
                .success(true)
                .message("Restaurant approved")
                .data(null)
                .build();
    }

    @PutMapping("/restaurants/{id}/reject")
    public ApiResponse<String> rejectRestaurant(@PathVariable Long id) {
        restaurantService.rejectRestaurant(id);
        return ApiResponse.<String>builder()
                .success(true)
                .message("Restaurant rejected")
                .data(null)
                .build();
    }

    @GetMapping("/orders")
    public ApiResponse<Page<OrderResponse>> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.<Page<OrderResponse>>builder()
                .success(true)
                .message("Orders fetched")
                .data(orderService.getAllOrders(page, size))
                .build();
    }

    @GetMapping("/orders/{id}")
    public ApiResponse<OrderResponse> getOrder(@PathVariable Long id) {
        return ApiResponse.<OrderResponse>builder()
                .success(true)
                .message("Order fetched")
                .data(orderService.getOrderById(id))
                .build();
    }

    @PutMapping("/orders/{id}/cancel")
    public ApiResponse<String> cancelOrder(@PathVariable Long id) {
        orderService.cancelOrderByAdmin(id);
        return ApiResponse.<String>builder()
                .success(true)
                .message("Order cancelled")
                .data(null)
                .build();
    }

    @PutMapping("/orders/{id}/assign-delivery-agent/{agentId}")
    public ApiResponse<OrderResponse> assignDeliveryAgent(@PathVariable Long id, @PathVariable Long agentId) {
        return ApiResponse.<OrderResponse>builder()
                .success(true)
                .message("Delivery agent assigned")
                .data(orderService.assignDeliveryAgent(id, agentId))
                .build();
    }

    @GetMapping("/delivery-agents")
    public ApiResponse<Page<UserResponse>> getDeliveryAgents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.<Page<UserResponse>>builder()
                .success(true)
                .message("Delivery agents fetched")
                .data(userService.getDeliveryAgents(page, size))
                .build();
    }

    @PutMapping("/delivery-agents/{id}/activate")
    public ApiResponse<String> activateAgent(@PathVariable Long id) {
        userService.activateAgent(id);
        return ApiResponse.<String>builder()
                .success(true)
                .message("Agent activated")
                .data(null)
                .build();
    }

    @PutMapping("/delivery-agents/{id}/deactivate")
    public ApiResponse<String> deactivateAgent(@PathVariable Long id) {
        userService.deactivateAgent(id);
        return ApiResponse.<String>builder()
                .success(true)
                .message("Agent deactivated")
                .data(null)
                .build();
    }

    @GetMapping("/dashboard")
    public ApiResponse<Map<String, Object>> dashboard() {
        return ApiResponse.<Map<String, Object>>builder()
                .success(true)
                .message("Dashboard fetched")
                .data(userService.getAdminDashboard())
                .build();
    }
}
