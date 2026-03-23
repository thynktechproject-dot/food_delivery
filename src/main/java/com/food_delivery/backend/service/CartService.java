package com.food_delivery.backend.service;

import com.food_delivery.backend.dto.*;

public interface CartService {

    CartResponse addToCart(Long userId, AddToCartRequest request);

    CartResponse getCart(Long userId);

    CartResponse removeItem(Long userId, Long menuItemId);

    CartResponse updateItemQuantity(Long userId, Long menuItemId, int quantity);

    CartResponse changeRestaurant(Long userId, Long restaurantId);

    void clearCart(Long userId);
}
