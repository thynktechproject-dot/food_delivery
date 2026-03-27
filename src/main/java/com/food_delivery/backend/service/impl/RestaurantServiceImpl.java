package com.food_delivery.backend.service.impl;

import com.food_delivery.backend.dto.CreateRestaurantRequest;
import com.food_delivery.backend.dto.RestaurantResponse;
import com.food_delivery.backend.entity.Restaurant;
import com.food_delivery.backend.entity.User;
import com.food_delivery.backend.enums.RestaurantStatus;
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
        log.info("Creating restaurant for ownerId={}", ownerId);

        if (restaurantRepository.findByOwnerIdAndActiveTrue(ownerId).isPresent()) {
            log.error("Restaurant already exists for ownerId={}", ownerId);
            throw new BadRequestException("Restaurant already exists for this owner");
        }

        User owner = userRepository.findByIdAndDeletedAtIsNull(ownerId)
                .orElseThrow(() -> {
                    log.error("Owner not found with id={}", ownerId);
                    return new ResourceNotFoundException("Owner not found");
                });

        Restaurant restaurant = RestaurantMapper.toEntity(request);
        restaurant.setOwner(owner);
        restaurant.setStatus(RestaurantStatus.PENDING);

        Restaurant saved = restaurantRepository.save(restaurant);
        log.info("Restaurant created with id={}", saved.getId());

        return RestaurantMapper.toResponse(saved);
    }

    @Override
    public RestaurantResponse getRestaurant(Long id, Long ownerId) {
        log.info("Fetching restaurant id={} for ownerId={}", id, ownerId);

        Restaurant restaurant = restaurantRepository.findByIdAndOwnerIdAndActiveTrue(id, ownerId)
                .orElseThrow(() -> {
                    log.error("Restaurant not found id={}, ownerId={}", id, ownerId);
                    return new ResourceNotFoundException("Restaurant not found");
                });

        return RestaurantMapper.toResponse(restaurant);
    }

    @Override
    public RestaurantResponse updateRestaurant(Long id, CreateRestaurantRequest request, Long ownerId) {
        log.info("Updating restaurant id={} for ownerId={}", id, ownerId);

        Restaurant restaurant = restaurantRepository.findByIdAndOwnerIdAndActiveTrue(id, ownerId)
                .orElseThrow(() -> {
                    log.error("Restaurant not found id={}, ownerId={}", id, ownerId);
                    return new ResourceNotFoundException("Restaurant not found");
                });

        restaurant.setName(request.getName());
        restaurant.setLocation(request.getLocation());
        restaurant.setCuisineType(request.getCuisineType());

        Restaurant updated = restaurantRepository.save(restaurant);
        log.info("Restaurant updated id={}", updated.getId());

        return RestaurantMapper.toResponse(updated);
    }

    @Override
    public void deleteRestaurant(Long id, Long ownerId) {
        log.warn("Deleting restaurant id={} for ownerId={}", id, ownerId);

        Restaurant restaurant = restaurantRepository.findByIdAndOwnerIdAndActiveTrue(id, ownerId)
                .orElseThrow(() -> {
                    log.error("Restaurant not found id={}, ownerId={}", id, ownerId);
                    return new ResourceNotFoundException("Restaurant not found");
                });

        archiveRestaurant(restaurant);
    }

    private void archiveRestaurant(Restaurant restaurant) {
        log.info("Archiving restaurant id={}", restaurant.getId());

        restaurant.setActive(false);
        restaurant.setDeletedAt(LocalDateTime.now());
        restaurantRepository.save(restaurant);
    }

    // Remaining methods → just added entry logs

    @Override
    public Page<RestaurantResponse> getAllRestaurants(int page, int size) {
        log.info("Fetching all restaurants page={}, size={}", page, size);
        Pageable pageable = PagingUtils.pageRequest(page, size, Sort.by(Sort.Direction.DESC, "id"));
        return restaurantRepository.findAll(pageable).map(RestaurantMapper::toResponse);
    }

    @Override
    public Page<RestaurantResponse> getApprovedRestaurants(String keyword, int page, int size) {
        log.info("Fetching approved restaurants keyword={}, page={}, size={}", keyword, page, size);

        Pageable pageable = PagingUtils.pageRequest(page, size, Sort.by(Sort.Direction.ASC, "name", "id"));

        if (keyword == null || keyword.isBlank()) {
            return restaurantRepository
                    .findByStatusAndActiveTrue(RestaurantStatus.APPROVED, pageable)
                    .map(RestaurantMapper::toResponse);
        }

        return restaurantRepository
                .findByStatusAndActiveTrueAndNameContainingIgnoreCase(
                        RestaurantStatus.APPROVED,
                        keyword.trim(),
                        pageable
                )
                .map(RestaurantMapper::toResponse);
    }

    @Override
    public RestaurantResponse getApprovedRestaurant(Long id) {
        log.info("Fetching approved restaurant id={}", id);

        Restaurant restaurant = restaurantRepository
                .findByIdAndStatusAndActiveTrue(id, RestaurantStatus.APPROVED)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found"));

        return RestaurantMapper.toResponse(restaurant);
    }

    @Override
    public Page<RestaurantResponse> searchRestaurants(String keyword, int page, int size) {
        log.info("Searching restaurants keyword={}", keyword);
        return getApprovedRestaurants(keyword, page, size);
    }

    @Override
    public Page<RestaurantResponse> getPendingRestaurants(int page, int size) {
        log.info("Fetching pending restaurants page={}, size={}", page, size);

        Pageable pageable = PagingUtils.pageRequest(page, size, Sort.by(Sort.Direction.DESC, "id"));

        return restaurantRepository
                .findByStatusAndActiveTrue(RestaurantStatus.PENDING, pageable)
                .map(RestaurantMapper::toResponse);
    }

    @Override
    public void approveRestaurant(Long id) {
        log.info("Approving restaurant id={}", id);

        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found"));

        if (!restaurant.isActive()) {
            log.error("Attempt to approve archived restaurant id={}", id);
            throw new BadRequestException("Archived restaurants cannot be approved");
        }

        restaurant.setStatus(RestaurantStatus.APPROVED);
        restaurantRepository.save(restaurant);
    }

    @Override
    public void rejectRestaurant(Long id) {
        log.warn("Rejecting restaurant id={}", id);

        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found"));

        restaurant.setStatus(RestaurantStatus.REJECTED);
        restaurant.setActive(false);

        restaurantRepository.save(restaurant);
    }
}