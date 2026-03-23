package com.food_delivery.backend.controller;

import com.food_delivery.backend.dto.ApiResponse;
import com.food_delivery.backend.dto.CreateRestaurantRequest;
import com.food_delivery.backend.dto.OrderResponse;
import com.food_delivery.backend.dto.RestaurantResponse;
import com.food_delivery.backend.service.OrderService;
import com.food_delivery.backend.service.RestaurantService;
import com.food_delivery.backend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/restaurant")
@RequiredArgsConstructor
@PreAuthorize("hasRole('RESTAURANT_OWNER')")
public class RestaurantController {

    private final RestaurantService restaurantService;
    private final OrderService orderService;
    private final UserService userService;

    @PostMapping
    public ApiResponse<RestaurantResponse> createRestaurant(
            @Valid @RequestBody CreateRestaurantRequest request,
            Authentication authentication
    ) {
        Long ownerId = resolveCurrentUserId(authentication);
        return ApiResponse.<RestaurantResponse>builder()
                .success(true)
                .message("Restaurant created")
                .data(restaurantService.createRestaurant(request, ownerId))
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<RestaurantResponse> getRestaurant(@PathVariable Long id, Authentication authentication) {
        Long ownerId = resolveCurrentUserId(authentication);
        return ApiResponse.<RestaurantResponse>builder()
                .success(true)
                .message("Restaurant fetched")
                .data(restaurantService.getRestaurant(id, ownerId))
                .build();
    }

    @PutMapping("/{id}")
    public ApiResponse<RestaurantResponse> updateRestaurant(
            @PathVariable Long id,
            @Valid @RequestBody CreateRestaurantRequest request,
            Authentication authentication
    ) {
        Long ownerId = resolveCurrentUserId(authentication);
        return ApiResponse.<RestaurantResponse>builder()
                .success(true)
                .message("Restaurant updated")
                .data(restaurantService.updateRestaurant(id, request, ownerId))
                .build();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<String> deleteRestaurant(@PathVariable Long id, Authentication authentication) {
        Long ownerId = resolveCurrentUserId(authentication);
        restaurantService.deleteRestaurant(id, ownerId);
        return ApiResponse.<String>builder()
                .success(true)
                .message("Restaurant deleted")
                .data(null)
                .build();
    }

    @GetMapping("/orders")
    public ApiResponse<Page<OrderResponse>> getRestaurantOrders(
            Authentication authentication,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "0") int page,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "20") int size
    ) {
        Long ownerId = resolveCurrentUserId(authentication);
        return ApiResponse.<Page<OrderResponse>>builder()
                .success(true)
                .message("Restaurant orders fetched")
                .data(orderService.getOrdersByRestaurantOwner(ownerId, page, size))
                .build();
    }

    @GetMapping("/orders/{orderId}")
    public ApiResponse<OrderResponse> getRestaurantOrder(@PathVariable Long orderId, Authentication authentication) {
        Long ownerId = resolveCurrentUserId(authentication);
        return ApiResponse.<OrderResponse>builder()
                .success(true)
                .message("Restaurant order fetched")
                .data(orderService.getOrderByRestaurantOwner(orderId, ownerId))
                .build();
    }

    private Long resolveCurrentUserId(Authentication authentication) {
        return userService.getUserByEmail(authentication.getName()).getId();
    }
}
