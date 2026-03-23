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

        log.info("Attempting to create restaurant for ownerId: {}", ownerId);

        User owner = userRepository.findByIdAndDeletedAtIsNull(ownerId)
                .orElseThrow(() -> {
                    log.error("Owner not found with id: {}", ownerId);
                    return new ResourceNotFoundException("Owner not found");
                });

        Restaurant restaurant = RestaurantMapper.toEntity(request);
        restaurant.setOwner(owner);
        restaurant.setApproved(false);

        Restaurant saved = restaurantRepository.save(restaurant);

        log.info("Restaurant created successfully with id: {} for ownerId: {}", saved.getId(), ownerId);

        return RestaurantMapper.toResponse(saved);
    }

    @Override
    public RestaurantResponse getRestaurant(Long id, Long ownerId) {

        log.info("Fetching restaurant with id: {} for ownerId: {}", id, ownerId);

        Restaurant restaurant = restaurantRepository.findByIdAndOwnerIdAndActiveTrue(id, ownerId)
                .orElseThrow(() -> {
                    log.error("Restaurant not found with id: {} for ownerId: {}", id, ownerId);
                    return new ResourceNotFoundException("Restaurant not found");
                });

        log.info("Restaurant fetched successfully with id: {}", id);

        return RestaurantMapper.toResponse(restaurant);
    }

    @Override
    public RestaurantResponse updateRestaurant(Long id, CreateRestaurantRequest request, Long ownerId) {

        log.info("Updating restaurant with id: {} for ownerId: {}", id, ownerId);

        Restaurant restaurant = restaurantRepository.findByIdAndOwnerIdAndActiveTrue(id, ownerId)
                .orElseThrow(() -> {
                    log.error("Restaurant not found for update with id: {} and ownerId: {}", id, ownerId);
                    return new ResourceNotFoundException("Restaurant not found");
                });

        restaurant.setName(request.getName());
        restaurant.setLocation(request.getLocation());
        restaurant.setCuisineType(request.getCuisineType());

        Restaurant updated = restaurantRepository.save(restaurant);

        log.info("Restaurant updated successfully with id: {}", id);

        return RestaurantMapper.toResponse(updated);
    }

    @Override
    public void deleteRestaurant(Long id, Long ownerId) {

        log.info("Deleting restaurant with id: {} for ownerId: {}", id, ownerId);

        Restaurant restaurant = restaurantRepository.findByIdAndOwnerIdAndActiveTrue(id, ownerId)
                .orElseThrow(() -> {
                    log.error("Restaurant not found for deletion with id: {} and ownerId: {}", id, ownerId);
                    return new ResourceNotFoundException("Restaurant not found");
                });

        archiveRestaurant(restaurant);

        log.info("Restaurant archived (deleted) successfully with id: {}", id);
    }

    @Override
    public Page<RestaurantResponse> getAllRestaurants(int page, int size) {

        log.info("Fetching all restaurants, page: {}, size: {}", page, size);

        Pageable pageable = PagingUtils.pageRequest(page, size, Sort.by(Sort.Direction.DESC, "id"));

        Page<RestaurantResponse> response = restaurantRepository.findAll(pageable)
                .map(RestaurantMapper::toResponse);

        log.info("Fetched {} restaurants", response.getNumberOfElements());

        return response;
    }

    @Override
    public Page<RestaurantResponse> getApprovedRestaurants(String keyword, int page, int size) {

        log.info("Fetching approved restaurants with keyword: {}, page: {}, size: {}", keyword, page, size);

        Pageable pageable = PagingUtils.pageRequest(page, size, Sort.by(Sort.Direction.ASC, "name", "id"));

        if (keyword == null || keyword.isBlank()) {
            log.debug("No keyword provided, fetching all approved restaurants");

            return restaurantRepository.findByApprovedTrueAndActiveTrue(pageable)
                    .map(RestaurantMapper::toResponse);
        }

        log.debug("Searching approved restaurants with keyword: {}", keyword);

        return restaurantRepository.findByApprovedTrueAndActiveTrueAndNameContainingIgnoreCase(keyword.trim(), pageable)
                .map(RestaurantMapper::toResponse);
    }

    @Override
    public RestaurantResponse getApprovedRestaurant(Long id) {

        log.info("Fetching approved restaurant with id: {}", id);

        Restaurant restaurant = restaurantRepository.findByIdAndApprovedTrueAndActiveTrue(id)
                .orElseThrow(() -> {
                    log.error("Approved restaurant not found with id: {}", id);
                    return new ResourceNotFoundException("Restaurant not found");
                });

        log.info("Approved restaurant fetched successfully with id: {}", id);

        return RestaurantMapper.toResponse(restaurant);
    }

    @Override
    public Page<RestaurantResponse> searchRestaurants(String keyword, int page, int size) {

        log.info("Searching restaurants with keyword: {}", keyword);

        return getApprovedRestaurants(keyword, page, size);
    }

    @Override
    public Page<RestaurantResponse> getPendingRestaurants(int page, int size) {

        log.info("Fetching pending restaurants, page: {}, size: {}", page, size);

        Pageable pageable = PagingUtils.pageRequest(page, size, Sort.by(Sort.Direction.DESC, "id"));

        return restaurantRepository.findByApprovedFalseAndActiveTrue(pageable)
                .map(RestaurantMapper::toResponse);
    }

    @Override
    public void approveRestaurant(Long id) {

        log.info("Approving restaurant with id: {}", id);

        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Restaurant not found for approval with id: {}", id);
                    return new ResourceNotFoundException("Restaurant not found");
                });

        if (!restaurant.isActive()) {
            log.error("Attempt to approve archived restaurant with id: {}", id);
            throw new BadRequestException("Archived restaurants cannot be approved");
        }

        restaurant.setApproved(true);
        restaurantRepository.save(restaurant);

        log.info("Restaurant approved successfully with id: {}", id);
    }

    @Override
    public void rejectRestaurant(Long id) {

        log.info("Rejecting restaurant with id: {}", id);

        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Restaurant not found for rejection with id: {}", id);
                    return new ResourceNotFoundException("Restaurant not found");
                });

        archiveRestaurant(restaurant);

        log.info("Restaurant rejected and archived with id: {}", id);
    }

    private void archiveRestaurant(Restaurant restaurant) {

        log.debug("Archiving restaurant with id: {}", restaurant.getId());

        restaurant.setActive(false);
        restaurant.setApproved(false);
        restaurant.setDeletedAt(LocalDateTime.now());

        restaurantRepository.save(restaurant);

        log.info("Restaurant archived successfully with id: {}", restaurant.getId());
    }
}