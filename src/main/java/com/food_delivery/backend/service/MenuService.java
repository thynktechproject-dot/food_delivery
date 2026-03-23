package com.food_delivery.backend.service;

import com.food_delivery.backend.dto.*;
import java.util.List;

public interface MenuService {

    MenuItemResponse addMenuItem(Long restaurantId, Long ownerId, CreateMenuItemRequest request);

    List<MenuItemResponse> getMenu(Long restaurantId, Long ownerId);

    List<MenuItemResponse> getPublicMenu(Long restaurantId);

    MenuItemResponse getMenuItem(Long menuItemId, Long ownerId);

    MenuItemResponse getPublicMenuItem(Long menuItemId);

    MenuItemResponse updateMenuItem(Long menuItemId, Long ownerId, CreateMenuItemRequest request);

    MenuItemResponse updateAvailability(Long menuItemId, Long ownerId, boolean available);

    void deleteMenuItem(Long menuItemId, Long ownerId);
}
