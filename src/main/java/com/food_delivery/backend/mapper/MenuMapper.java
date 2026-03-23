package com.food_delivery.backend.mapper;

import com.food_delivery.backend.dto.*;
import com.food_delivery.backend.entity.MenuItem;

public class MenuMapper {

    public static MenuItem toEntity(CreateMenuItemRequest request) {
        return MenuItem.builder()
                .name(request.getName())
                .price(request.getPrice())
                .available(true)
                .build();
    }

    public static MenuItemResponse toResponse(MenuItem item) {
        return MenuItemResponse.builder()
                .id(item.getId())
                .restaurantId(item.getRestaurant() != null ? item.getRestaurant().getId() : null)
                .name(item.getName())
                .price(item.getPrice())
                .available(item.isAvailable())
                .build();
    }
}
