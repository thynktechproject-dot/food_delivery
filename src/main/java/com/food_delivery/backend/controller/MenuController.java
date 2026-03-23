package com.food_delivery.backend.controller;

import com.food_delivery.backend.dto.*;
import com.food_delivery.backend.service.MenuService;
import com.food_delivery.backend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/menu")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;
    private final UserService userService;

    @PostMapping("/{restaurantId}")
    @PreAuthorize("hasRole('RESTAURANT_OWNER')")
    public ApiResponse<MenuItemResponse> addItem(
            @PathVariable Long restaurantId,
            @Valid @RequestBody CreateMenuItemRequest request,
            Authentication authentication
    ) {
        Long ownerId = resolveCurrentUserId(authentication);
        return ApiResponse.<MenuItemResponse>builder()
                .success(true)
                .message("Menu item added")
                .data(menuService.addMenuItem(restaurantId, ownerId, request))
                .build();
    }

    @GetMapping("/{restaurantId}")
    @PreAuthorize("hasRole('RESTAURANT_OWNER')")
    public ApiResponse<List<MenuItemResponse>> getMenu(@PathVariable Long restaurantId, Authentication authentication) {
        Long ownerId = resolveCurrentUserId(authentication);
        return ApiResponse.<List<MenuItemResponse>>builder()
                .success(true)
                .message("Menu fetched")
                .data(menuService.getMenu(restaurantId, ownerId))
                .build();
    }

    @GetMapping("/item/{menuItemId}")
    @PreAuthorize("hasRole('RESTAURANT_OWNER')")
    public ApiResponse<MenuItemResponse> getMenuItem(@PathVariable Long menuItemId, Authentication authentication) {
        Long ownerId = resolveCurrentUserId(authentication);
        return ApiResponse.<MenuItemResponse>builder()
                .success(true)
                .message("Menu item fetched")
                .data(menuService.getMenuItem(menuItemId, ownerId))
                .build();
    }

    @PutMapping("/item/{menuItemId}")
    @PreAuthorize("hasRole('RESTAURANT_OWNER')")
    public ApiResponse<MenuItemResponse> updateMenuItem(
            @PathVariable Long menuItemId,
            @Valid @RequestBody CreateMenuItemRequest request,
            Authentication authentication
    ) {
        Long ownerId = resolveCurrentUserId(authentication);
        return ApiResponse.<MenuItemResponse>builder()
                .success(true)
                .message("Menu item updated")
                .data(menuService.updateMenuItem(menuItemId, ownerId, request))
                .build();
    }

    @PutMapping("/item/{menuItemId}/availability")
    @PreAuthorize("hasRole('RESTAURANT_OWNER')")
    public ApiResponse<MenuItemResponse> updateAvailability(
            @PathVariable Long menuItemId,
            @Valid @RequestBody UpdateAvailabilityRequest request,
            Authentication authentication
    ) {
        Long ownerId = resolveCurrentUserId(authentication);
        return ApiResponse.<MenuItemResponse>builder()
                .success(true)
                .message("Menu item availability updated")
                .data(menuService.updateAvailability(menuItemId, ownerId, request.getAvailable()))
                .build();
    }

    @DeleteMapping("/item/{menuItemId}")
    @PreAuthorize("hasRole('RESTAURANT_OWNER')")
    public ApiResponse<String> deleteMenuItem(@PathVariable Long menuItemId, Authentication authentication) {
        Long ownerId = resolveCurrentUserId(authentication);
        menuService.deleteMenuItem(menuItemId, ownerId);
        return ApiResponse.<String>builder()
                .success(true)
                .message("Menu item deleted")
                .data(null)
                .build();
    }

    private Long resolveCurrentUserId(Authentication authentication) {
        return userService.getUserByEmail(authentication.getName()).getId();
    }
}
