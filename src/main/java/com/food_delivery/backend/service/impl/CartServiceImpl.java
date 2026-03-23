package com.food_delivery.backend.service.impl;

import com.food_delivery.backend.dto.*;
import com.food_delivery.backend.entity.*;
import com.food_delivery.backend.exception.BadRequestException;
import com.food_delivery.backend.exception.ResourceNotFoundException;
import com.food_delivery.backend.repository.*;
import com.food_delivery.backend.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final MenuItemRepository menuRepository;

    @Override
    public CartResponse addToCart(Long userId, AddToCartRequest request) {

        Cart cart = cartRepository.findByUserId(userId)
                .orElseGet(() -> Cart.builder()
                        .userId(userId)
                        .restaurantId(request.getRestaurantId())
                        .items(new ArrayList<>())
                        .build());

        if (cart.getRestaurantId() == null) {
            cart.setRestaurantId(request.getRestaurantId());
        }

        // RULE: One restaurant per cart
        if (!cart.getItems().isEmpty() &&
                !cart.getRestaurantId().equals(request.getRestaurantId())) {
            throw new BadRequestException("Cart already contains items from another restaurant");
        }

        MenuItem menuItem = menuRepository.findById(request.getMenuItemId())
                .orElseThrow(() -> new ResourceNotFoundException("Menu item not found"));

        if (menuItem.getRestaurant() == null
                || !menuItem.getRestaurant().isApproved()
                || !menuItem.getRestaurant().isActive()) {
            throw new BadRequestException("Menu item belongs to a restaurant that is not available");
        }

        if (!menuItem.isAvailable()) {
            throw new BadRequestException("Menu item is currently unavailable");
        }

        if (!menuItem.getRestaurant().getId().equals(request.getRestaurantId())) {
            throw new BadRequestException("Restaurant does not match the selected menu item");
        }

        Optional<CartItem> existingItem = cart.getItems()
                .stream()
                .filter(i -> i.getMenuItemId().equals(request.getMenuItemId()))
                .findFirst();

        if (existingItem.isPresent()) {
            existingItem.get().setQuantity(
                    existingItem.get().getQuantity() + request.getQuantity()
            );
        } else {
            CartItem newItem = CartItem.builder()
                    .menuItemId(menuItem.getId())
                    .name(menuItem.getName())
                    .price(menuItem.getPrice())
                    .quantity(request.getQuantity())
                    .cart(cart)
                    .build();

            cart.getItems().add(newItem);
        }

        Cart saved = cartRepository.save(cart);

        return mapToResponse(saved);
    }

    @Override
    public CartResponse getCart(Long userId) {

        Cart cart = cartRepository.findByUserId(userId)
                .orElseGet(() -> Cart.builder()
                        .userId(userId)
                        .items(List.of())
                        .build());

        return mapToResponse(cart);
    }

    @Override
    public CartResponse removeItem(Long userId, Long menuItemId) {

        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));

        cart.getItems().removeIf(item -> item.getMenuItemId().equals(menuItemId));
        if (cart.getItems().isEmpty()) {
            cart.setRestaurantId(null);
        }

        return mapToResponse(cartRepository.save(cart));
    }

    @Override
    public void clearCart(Long userId) {

        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));

        cart.getItems().clear();
        cart.setRestaurantId(null);
        cartRepository.save(cart);
    }

    private CartResponse mapToResponse(Cart cart) {

        List<CartItemResponse> items = cart.getItems().stream()
                .map(item -> CartItemResponse.builder()
                        .menuItemId(item.getMenuItemId())
                        .name(item.getName())
                        .price(item.getPrice())
                        .quantity(item.getQuantity())
                        .build())
                .toList();

        double total = items.stream()
                .mapToDouble(i -> i.getPrice() * i.getQuantity())
                .sum();

        return CartResponse.builder()
                .userId(cart.getUserId())
                .restaurantId(cart.getRestaurantId())
                .items(items)
                .totalAmount(total)
                .build();
    }
}
