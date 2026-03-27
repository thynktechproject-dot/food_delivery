package com.food_delivery.backend.controller;

import com.food_delivery.backend.dto.ApiResponse;
import com.food_delivery.backend.dto.MenuItemResponse;
import com.food_delivery.backend.dto.RestaurantResponse;
import com.food_delivery.backend.service.MenuService;
import com.food_delivery.backend.service.RestaurantService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/public/restaurants")
@RequiredArgsConstructor
@Slf4j
public class PublicRestaurantController {

    private final RestaurantService restaurantService;
    private final MenuService menuService;

    @GetMapping
    public ApiResponse<Page<RestaurantResponse>> getApprovedRestaurants(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        log.info("GET /api/public/restaurants called");
        log.debug("Fetching restaurants with keyword: {}, page: {}, size: {}", keyword, page, size);

        return ApiResponse.<Page<RestaurantResponse>>builder()
                .success(true)
                .message("Restaurants fetched")
                .data(restaurantService.getApprovedRestaurants(keyword, page, size))
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<RestaurantResponse> getApprovedRestaurant(@PathVariable Long id) {
        log.info("GET /api/public/restaurants/{} called", id);
        log.debug("Fetching restaurant with id: {}", id);

        return ApiResponse.<RestaurantResponse>builder()
                .success(true)
                .message("Restaurant fetched")
                .data(restaurantService.getApprovedRestaurant(id))
                .build();
    }

    @GetMapping("/{id}/menu")
    public ApiResponse<List<MenuItemResponse>> getPublicMenu(@PathVariable Long id) {
        log.info("GET /api/public/restaurants/{}/menu called", id);
        log.debug("Fetching menu for restaurantId: {}", id);

        return ApiResponse.<List<MenuItemResponse>>builder()
                .success(true)
                .message("Menu fetched")
                .data(menuService.getPublicMenu(id))
                .build();
    }

    @GetMapping("/menu-items/{menuItemId}")
    public ApiResponse<MenuItemResponse> getPublicMenuItem(@PathVariable Long menuItemId) {
        log.info("GET /api/public/restaurants/menu-items/{} called", menuItemId);
        log.debug("Fetching menuItemId: {}", menuItemId);

        return ApiResponse.<MenuItemResponse>builder()
                .success(true)
                .message("Menu item fetched")
                .data(menuService.getPublicMenuItem(menuItemId))
                .build();
    }
}
