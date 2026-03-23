package com.food_delivery.backend.controller;

import com.food_delivery.backend.dto.AddToCartRequest;
import com.food_delivery.backend.dto.ApiResponse;
import com.food_delivery.backend.dto.CartResponse;
import com.food_delivery.backend.service.UserService;
import com.food_delivery.backend.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user/cart")
@RequiredArgsConstructor
@PreAuthorize("hasRole('USER')")
public class CartController {

    private final CartService cartService;
    private final UserService userService;

    @PostMapping
    public ApiResponse<CartResponse> addToCart(
            @Valid @RequestBody AddToCartRequest request,
            Authentication authentication
    ) {
        Long userId = resolveCurrentUserId(authentication);
        return ApiResponse.<CartResponse>builder()
                .success(true)
                .message("Item added to cart")
                .data(cartService.addToCart(userId, request))
                .build();
    }

    @GetMapping
    public ApiResponse<CartResponse> getCart(Authentication authentication) {
        Long userId = resolveCurrentUserId(authentication);
        return ApiResponse.<CartResponse>builder()
                .success(true)
                .message("Cart fetched")
                .data(cartService.getCart(userId))
                .build();
    }

    @DeleteMapping("/item/{menuItemId}")
    public ApiResponse<CartResponse> removeItem(
            Authentication authentication,
            @PathVariable Long menuItemId
    ) {
        Long userId = resolveCurrentUserId(authentication);
        return ApiResponse.<CartResponse>builder()
                .success(true)
                .message("Item removed")
                .data(cartService.removeItem(userId, menuItemId))
                .build();
    }

    @DeleteMapping("/clear")
    public ApiResponse<String> clearCart(Authentication authentication) {
        Long userId = resolveCurrentUserId(authentication);
        cartService.clearCart(userId);
        return ApiResponse.<String>builder()
                .success(true)
                .message("Cart cleared")
                .data(null)
                .build();
    }

    private Long resolveCurrentUserId(Authentication authentication) {
        return userService.getUserByEmail(authentication.getName()).getId();
    }
}
