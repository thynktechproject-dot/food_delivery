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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/restaurant")
@RequiredArgsConstructor
@PreAuthorize("hasRole('RESTAURANT_OWNER')")
public class RestaurantController {

    private static final Logger log = LoggerFactory.getLogger(RestaurantController.class);

    private final RestaurantService restaurantService;
    private final OrderService orderService;
    private final UserService userService;

    @PostMapping
    public ApiResponse<RestaurantResponse> createRestaurant(
            @Valid @RequestBody CreateRestaurantRequest request,
            Authentication authentication
    ) {
        Long ownerId = resolveCurrentUserId(authentication);
        log.info("Creating restaurant for ownerId: {}", ownerId);

        RestaurantResponse response = restaurantService.createRestaurant(request, ownerId);

        log.info("Restaurant created successfully for ownerId: {}", ownerId);

        return ApiResponse.<RestaurantResponse>builder()
                .success(true)
                .message("Restaurant created")
                .data(response)
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<RestaurantResponse> getRestaurant(@PathVariable Long id, Authentication authentication) {
        Long ownerId = resolveCurrentUserId(authentication);
        log.info("Fetching restaurant with id: {} for ownerId: {}", id, ownerId);

        RestaurantResponse response = restaurantService.getRestaurant(id, ownerId);

        log.info("Restaurant fetched successfully with id: {}", id);

        return ApiResponse.<RestaurantResponse>builder()
                .success(true)
                .message("Restaurant fetched")
                .data(response)
                .build();
    }

    @PutMapping("/{id}")
    public ApiResponse<RestaurantResponse> updateRestaurant(
            @PathVariable Long id,
            @Valid @RequestBody CreateRestaurantRequest request,
            Authentication authentication
    ) {
        Long ownerId = resolveCurrentUserId(authentication);
        log.info("Updating restaurant with id: {} for ownerId: {}", id, ownerId);

        RestaurantResponse response = restaurantService.updateRestaurant(id, request, ownerId);

        log.info("Restaurant updated successfully with id: {}", id);

        return ApiResponse.<RestaurantResponse>builder()
                .success(true)
                .message("Restaurant updated")
                .data(response)
                .build();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<String> deleteRestaurant(@PathVariable Long id, Authentication authentication) {
        Long ownerId = resolveCurrentUserId(authentication);
        log.info("Deleting restaurant with id: {} for ownerId: {}", id, ownerId);

        restaurantService.deleteRestaurant(id, ownerId);

        log.info("Restaurant deleted successfully with id: {}", id);

        return ApiResponse.<String>builder()
                .success(true)
                .message("Restaurant deleted")
                .data(null)
                .build();
    }

    @GetMapping("/orders")
    public ApiResponse<Page<OrderResponse>> getRestaurantOrders(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Long ownerId = resolveCurrentUserId(authentication);
        log.info("Fetching orders for ownerId: {}, page: {}, size: {}", ownerId, page, size);

        Page<OrderResponse> response = orderService.getOrdersByRestaurantOwner(ownerId, page, size);

        log.info("Orders fetched successfully for ownerId: {}", ownerId);

        return ApiResponse.<Page<OrderResponse>>builder()
                .success(true)
                .message("Restaurant orders fetched")
                .data(response)
                .build();
    }

    @GetMapping("/orders/{orderId}")
    public ApiResponse<OrderResponse> getRestaurantOrder(@PathVariable Long orderId, Authentication authentication) {
        Long ownerId = resolveCurrentUserId(authentication);
        log.info("Fetching order with id: {} for ownerId: {}", orderId, ownerId);

        OrderResponse response = orderService.getOrderByRestaurantOwner(orderId, ownerId);

        log.info("Order fetched successfully with id: {}", orderId);

        return ApiResponse.<OrderResponse>builder()
                .success(true)
                .message("Restaurant order fetched")
                .data(response)
                .build();
    }

    private Long resolveCurrentUserId(Authentication authentication) {
        log.debug("Resolving user from authentication: {}", authentication.getName());
        return userService.getUserByEmail(authentication.getName()).getId();
    }
}