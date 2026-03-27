package com.food_delivery.backend.service.impl;

import com.food_delivery.backend.dto.*;
import com.food_delivery.backend.entity.*;
import com.food_delivery.backend.enums.RestaurantStatus;
import com.food_delivery.backend.exception.BadRequestException;
import com.food_delivery.backend.exception.ResourceNotFoundException;
import com.food_delivery.backend.repository.*;
import com.food_delivery.backend.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final MenuItemRepository menuRepository;

    @Override
    public CartResponse addToCart(Long userId, AddToCartRequest request) {

        log.info("Add to cart request for userId: {}", userId);
        log.debug("Adding menuItemId: {} with quantity: {} for restaurantId: {}",
                request.getMenuItemId(), request.getQuantity(), request.getRestaurantId());

        Cart cart = cartRepository.findByUserId(userId)
                .orElseGet(() -> {
                    log.debug("Creating new cart for userId: {}", userId);
                    return Cart.builder()
                            .userId(userId)
                            .restaurantId(request.getRestaurantId())
                            .items(new ArrayList<>())
                            .build();
                });

        if (cart.getRestaurantId() == null) {
            log.debug("Setting restaurantId: {} for cart", request.getRestaurantId());
            cart.setRestaurantId(request.getRestaurantId());
        }

        if (!cart.getItems().isEmpty() &&
                !cart.getRestaurantId().equals(request.getRestaurantId())) {
            log.warn("Cart conflict for userId: {} - multiple restaurants not allowed", userId);
            throw new BadRequestException("Cart already contains items from another restaurant");
        }

        MenuItem menuItem = menuRepository.findById(request.getMenuItemId())
                .orElseThrow(() -> {
                    log.warn("Menu item not found: {}", request.getMenuItemId());
                    return new ResourceNotFoundException("Menu item not found");
                });

        if (menuItem.getRestaurant() == null
                || menuItem.getRestaurant().getStatus() != RestaurantStatus.APPROVED
                || !menuItem.getRestaurant().isActive()) {
            log.warn("Restaurant not available for menuItemId: {}", menuItem.getId());
            throw new BadRequestException("Menu item belongs to a restaurant that is not available");
        }

        if (!menuItem.isAvailable()) {
            log.warn("Menu item unavailable: {}", menuItem.getId());
            throw new BadRequestException("Menu item is currently unavailable");
        }

        if (!menuItem.getRestaurant().getId().equals(request.getRestaurantId())) {
            log.warn("Restaurant mismatch for menuItemId: {}", menuItem.getId());
            throw new BadRequestException("Restaurant does not match the selected menu item");
        }

        Optional<CartItem> existingItem = cart.getItems()
                .stream()
                .filter(i -> i.getMenuItemId().equals(request.getMenuItemId()))
                .findFirst();

        if (existingItem.isPresent()) {
            log.debug("Updating quantity for menuItemId: {}", request.getMenuItemId());
            existingItem.get().setQuantity(
                    existingItem.get().getQuantity() + request.getQuantity()
            );
        } else {
            log.debug("Adding new item to cart: {}", request.getMenuItemId());

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

        log.info("Cart updated successfully for userId: {}", userId);

        return mapToResponse(saved);
    }

    @Override
    public CartResponse getCart(Long userId) {

        log.info("Fetching cart for userId: {}", userId);

        Cart cart = cartRepository.findByUserId(userId)
                .orElseGet(() -> {
                    log.debug("No cart found, returning empty cart for userId: {}", userId);
                    return Cart.builder()
                            .userId(userId)
                            .items(List.of())
                            .build();
                });

        return mapToResponse(cart);
    }

    @Override
    public CartResponse removeItem(Long userId, Long menuItemId) {

        log.info("Removing item from cart for userId: {}", userId);
        log.debug("Removing menuItemId: {}", menuItemId);

        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> {
                    log.warn("Cart not found for userId: {}", userId);
                    return new ResourceNotFoundException("Cart not found");
                });

        cart.getItems().removeIf(item -> item.getMenuItemId().equals(menuItemId));

        if (cart.getItems().isEmpty()) {
            log.debug("Cart is empty, resetting restaurantId for userId: {}", userId);
            cart.setRestaurantId(null);
        }

        log.info("Item removed successfully from cart for userId: {}", userId);

        return mapToResponse(cartRepository.save(cart));
    }

    @Override
    public void clearCart(Long userId) {

        log.info("Clearing cart for userId: {}", userId);

        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> {
                    log.warn("Cart not found for userId: {}", userId);
                    return new ResourceNotFoundException("Cart not found");
                });

        cart.getItems().clear();
        cart.setRestaurantId(null);

        cartRepository.save(cart);

        log.info("Cart cleared successfully for userId: {}", userId);
    }

    private CartResponse mapToResponse(Cart cart) {

        log.debug("Mapping cart to response for userId: {}", cart.getUserId());

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

        log.debug("Cart total calculated: {} for userId: {}", total, cart.getUserId());

        return CartResponse.builder()
                .userId(cart.getUserId())
                .restaurantId(cart.getRestaurantId())
                .items(items)
                .totalAmount(total)
                .build();
    }
}
