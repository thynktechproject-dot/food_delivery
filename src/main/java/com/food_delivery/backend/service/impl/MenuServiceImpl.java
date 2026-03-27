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
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MenuServiceImpl implements MenuService {

    private final MenuItemRepository menuRepository;
    private final RestaurantRepository restaurantRepository;

    @Override
    public MenuItemResponse addMenuItem(Long restaurantId, Long ownerId, CreateMenuItemRequest request) {
        log.info("Adding menu item for restaurantId={}, ownerId={}", restaurantId, ownerId);

        Restaurant restaurant = restaurantRepository.findByIdAndOwnerIdAndActiveTrue(restaurantId, ownerId)
                .orElseThrow(() -> {
                    log.error("Restaurant not found restaurantId={}, ownerId={}", restaurantId, ownerId);
                    return new ResourceNotFoundException("Restaurant not found");
                });

        MenuItem item = MenuMapper.toEntity(request);
        item.setRestaurant(restaurant);

        MenuItem saved = menuRepository.save(item);
        log.info("Menu item created id={}", saved.getId());

        return MenuMapper.toResponse(saved);
    }

    @Override
    public List<MenuItemResponse> getMenu(Long restaurantId, Long ownerId) {
        log.info("Fetching menu for restaurantId={}, ownerId={}", restaurantId, ownerId);

        restaurantRepository.findByIdAndOwnerIdAndActiveTrue(restaurantId, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found"));

        return menuRepository.findByRestaurantId(restaurantId)
                .stream()
                .map(MenuMapper::toResponse)
                .toList();
    }

    @Override
    public List<MenuItemResponse> getPublicMenu(Long restaurantId) {
        log.info("Fetching public menu for restaurantId={}", restaurantId);

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
        log.info("Fetching menuItemId={} for ownerId={}", menuItemId, ownerId);

        MenuItem item = getMenuItemOrThrow(menuItemId);
        validateOwner(item, ownerId);
        return MenuMapper.toResponse(item);
    }

    @Override
    public MenuItemResponse getPublicMenuItem(Long menuItemId) {
        log.info("Fetching public menuItemId={}", menuItemId);

        MenuItem item = getMenuItemOrThrow(menuItemId);

        if (!item.isAvailable()
                || item.getRestaurant() == null
                || item.getRestaurant().getStatus() != RestaurantStatus.APPROVED
                || !item.getRestaurant().isActive()) {
            log.error("Menu item not accessible id={}", menuItemId);
            throw new ResourceNotFoundException("Menu item not found");
        }

        return MenuMapper.toResponse(item);
    }

    @Override
    public MenuItemResponse updateMenuItem(Long menuItemId, Long ownerId, CreateMenuItemRequest request) {
        log.info("Updating menuItemId={} for ownerId={}", menuItemId, ownerId);

        MenuItem item = getMenuItemOrThrow(menuItemId);
        validateOwner(item, ownerId);

        item.setName(request.getName());
        item.setPrice(request.getPrice());

        MenuItem updated = menuRepository.save(item);
        return MenuMapper.toResponse(updated);
    }

    @Override
    public MenuItemResponse updateAvailability(Long menuItemId, Long ownerId, boolean available) {
        log.info("Updating availability menuItemId={} to {}", menuItemId, available);

        MenuItem item = getMenuItemOrThrow(menuItemId);
        validateOwner(item, ownerId);

        item.setAvailable(available);
        return MenuMapper.toResponse(menuRepository.save(item));
    }

    @Override
    public void deleteMenuItem(Long menuItemId, Long ownerId) {
        log.warn("Deleting menuItemId={} by ownerId={}", menuItemId, ownerId);

        MenuItem item = getMenuItemOrThrow(menuItemId);
        validateOwner(item, ownerId);

        menuRepository.delete(item);
    }

    private MenuItem getMenuItemOrThrow(Long menuItemId) {
        return menuRepository.findById(menuItemId)
                .orElseThrow(() -> {
                    log.error("Menu item not found id={}", menuItemId);
                    return new ResourceNotFoundException("Menu item not found");
                });
    }

    private void validateOwner(MenuItem item, Long ownerId) {
        Long restaurantId = item.getRestaurant() != null ? item.getRestaurant().getId() : null;

        if (restaurantId == null || restaurantRepository.findByIdAndOwnerIdAndActiveTrue(restaurantId, ownerId).isEmpty()) {
            log.error("Unauthorized access by ownerId={} for restaurantId={}", ownerId, restaurantId);
            throw new ForbiddenException("You can only manage menu items for your own restaurant");
        }
    }
}