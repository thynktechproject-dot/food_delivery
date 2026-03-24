package com.food_delivery.backend.service.impl;

import com.food_delivery.backend.dto.*;
import com.food_delivery.backend.entity.*;
import com.food_delivery.backend.enums.RestaurantStatus;
import com.food_delivery.backend.exception.ForbiddenException;
import com.food_delivery.backend.exception.ResourceNotFoundException;
import com.food_delivery.backend.mapper.MenuMapper;
import com.food_delivery.backend.repository.*;
import com.food_delivery.backend.service.MenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MenuServiceImpl implements MenuService {

    private final MenuItemRepository menuRepository;
    private final RestaurantRepository restaurantRepository;

    @Override
    public MenuItemResponse addMenuItem(Long restaurantId, Long ownerId, CreateMenuItemRequest request) {

        Restaurant restaurant = restaurantRepository.findByIdAndOwnerIdAndActiveTrue(restaurantId, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found"));

        MenuItem item = MenuMapper.toEntity(request);
        item.setRestaurant(restaurant);

        MenuItem saved = menuRepository.save(item);

        return MenuMapper.toResponse(saved);
    }

    @Override
    public List<MenuItemResponse> getMenu(Long restaurantId, Long ownerId) {
        restaurantRepository.findByIdAndOwnerIdAndActiveTrue(restaurantId, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found"));

        return menuRepository.findByRestaurantId(restaurantId)
                .stream()
                .map(MenuMapper::toResponse)
                .toList();
    }

    @Override
    public List<MenuItemResponse> getPublicMenu(Long restaurantId) {

        Restaurant restaurant = restaurantRepository
                .findByIdAndStatusAndActiveTrue(restaurantId, RestaurantStatus.APPROVED)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found"));

        return menuRepository.findByRestaurantIdAndAvailableTrue(restaurant.getId())
                .stream()
                .map(MenuMapper::toResponse)
                .toList();
    }

    @Override
    public MenuItemResponse getMenuItem(Long menuItemId, Long ownerId) {
        MenuItem item = getMenuItemOrThrow(menuItemId);
        validateOwner(item, ownerId);
        return MenuMapper.toResponse(item);
    }

    @Override
    public MenuItemResponse getPublicMenuItem(Long menuItemId) {

        MenuItem item = getMenuItemOrThrow(menuItemId);

        if (!item.isAvailable()
                || item.getRestaurant() == null
                || item.getRestaurant().getStatus() != RestaurantStatus.APPROVED
                || !item.getRestaurant().isActive()) {
            throw new ResourceNotFoundException("Menu item not found");
        }

        return MenuMapper.toResponse(item);
    }

    @Override
    public MenuItemResponse updateMenuItem(Long menuItemId, Long ownerId, CreateMenuItemRequest request) {
        MenuItem item = getMenuItemOrThrow(menuItemId);
        validateOwner(item, ownerId);
        item.setName(request.getName());
        item.setPrice(request.getPrice());
        MenuItem updated = menuRepository.save(item);
        return MenuMapper.toResponse(updated);
    }

    @Override
    public MenuItemResponse updateAvailability(Long menuItemId, Long ownerId, boolean available) {
        MenuItem item = getMenuItemOrThrow(menuItemId);
        validateOwner(item, ownerId);
        item.setAvailable(available);
        return MenuMapper.toResponse(menuRepository.save(item));
    }

    @Override
    public void deleteMenuItem(Long menuItemId, Long ownerId) {
        MenuItem item = getMenuItemOrThrow(menuItemId);
        validateOwner(item, ownerId);
        menuRepository.delete(item);
    }

    private MenuItem getMenuItemOrThrow(Long menuItemId) {
        return menuRepository.findById(menuItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Menu item not found"));
    }

    private void validateOwner(MenuItem item, Long ownerId) {
        Long restaurantId = item.getRestaurant() != null ? item.getRestaurant().getId() : null;
        if (restaurantId == null || restaurantRepository.findByIdAndOwnerIdAndActiveTrue(restaurantId, ownerId).isEmpty()) {
            throw new ForbiddenException("You can only manage menu items for your own restaurant");
        }
    }
}
