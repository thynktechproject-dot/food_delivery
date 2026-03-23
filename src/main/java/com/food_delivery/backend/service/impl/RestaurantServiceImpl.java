package com.food_delivery.backend.service.impl;

import com.food_delivery.backend.dto.*;
import com.food_delivery.backend.entity.Restaurant;
import com.food_delivery.backend.entity.User;
import com.food_delivery.backend.exception.BadRequestException;
import com.food_delivery.backend.exception.ResourceNotFoundException;
import com.food_delivery.backend.mapper.RestaurantMapper;
import com.food_delivery.backend.repository.RestaurantRepository;
import com.food_delivery.backend.repository.UserRepository;
import com.food_delivery.backend.service.RestaurantService;
import com.food_delivery.backend.util.PagingUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class RestaurantServiceImpl implements RestaurantService {

    private final RestaurantRepository restaurantRepository;
    private final UserRepository userRepository;

    @Override
    public RestaurantResponse createRestaurant(CreateRestaurantRequest request, Long ownerId) {

        if (restaurantRepository.findByOwnerIdAndActiveTrue(ownerId).isPresent()) {
            throw new BadRequestException("Restaurant already exists for this owner");
        }

        User owner = userRepository.findByIdAndDeletedAtIsNull(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Owner not found"));

        Restaurant restaurant = RestaurantMapper.toEntity(request);
        restaurant.setOwner(owner);
        restaurant.setApproved(false);
        Restaurant saved = restaurantRepository.save(restaurant);

        return RestaurantMapper.toResponse(saved);
    }

    @Override
    public RestaurantResponse getRestaurant(Long id, Long ownerId) {

        Restaurant restaurant = restaurantRepository.findByIdAndOwnerIdAndActiveTrue(id, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found"));

        return RestaurantMapper.toResponse(restaurant);
    }

    @Override
    public RestaurantResponse updateRestaurant(Long id, CreateRestaurantRequest request, Long ownerId) {

        Restaurant restaurant = restaurantRepository.findByIdAndOwnerIdAndActiveTrue(id, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found"));

        restaurant.setName(request.getName());
        restaurant.setLocation(request.getLocation());
        restaurant.setCuisineType(request.getCuisineType());

        Restaurant updated = restaurantRepository.save(restaurant);
        return RestaurantMapper.toResponse(updated);
    }

    @Override
    public void deleteRestaurant(Long id, Long ownerId) {
        Restaurant restaurant = restaurantRepository.findByIdAndOwnerIdAndActiveTrue(id, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found"));
        archiveRestaurant(restaurant);
    }

    @Override
    public Page<RestaurantResponse> getAllRestaurants(int page, int size) {
        Pageable pageable = PagingUtils.pageRequest(page, size, Sort.by(Sort.Direction.DESC, "id"));

        return restaurantRepository.findAll(pageable)
                .map(RestaurantMapper::toResponse);
    }

    @Override
    public Page<RestaurantResponse> getApprovedRestaurants(String keyword, int page, int size) {
        Pageable pageable = PagingUtils.pageRequest(page, size, Sort.by(Sort.Direction.ASC, "name", "id"));

        if (keyword == null || keyword.isBlank()) {
            return restaurantRepository.findByApprovedTrueAndActiveTrue(pageable)
                    .map(RestaurantMapper::toResponse);
        }

        return restaurantRepository.findByApprovedTrueAndActiveTrueAndNameContainingIgnoreCase(keyword.trim(), pageable)
                .map(RestaurantMapper::toResponse);
    }

    @Override
    public RestaurantResponse getApprovedRestaurant(Long id) {
        Restaurant restaurant = restaurantRepository.findByIdAndApprovedTrueAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found"));

        return RestaurantMapper.toResponse(restaurant);
    }

    @Override
    public Page<RestaurantResponse> searchRestaurants(String keyword, int page, int size) {
        return getApprovedRestaurants(keyword, page, size);
    }

    @Override
    public Page<RestaurantResponse> getPendingRestaurants(int page, int size) {
        Pageable pageable = PagingUtils.pageRequest(page, size, Sort.by(Sort.Direction.DESC, "id"));
        return restaurantRepository.findByApprovedFalseAndActiveTrue(pageable)
                .map(RestaurantMapper::toResponse);
    }

    @Override
    public void approveRestaurant(Long id) {
        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found"));

        if (!restaurant.isActive()) {
            throw new BadRequestException("Archived restaurants cannot be approved");
        }
        restaurant.setApproved(true);
        restaurantRepository.save(restaurant);
    }

    @Override
    public void rejectRestaurant(Long id) {
        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found"));

        archiveRestaurant(restaurant);
    }

    private void archiveRestaurant(Restaurant restaurant) {
        restaurant.setActive(false);
        restaurant.setApproved(false);
        restaurant.setDeletedAt(LocalDateTime.now());
        restaurantRepository.save(restaurant);
    }
}
