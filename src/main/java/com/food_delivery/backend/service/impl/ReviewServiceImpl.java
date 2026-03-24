package com.food_delivery.backend.service.impl;

import com.food_delivery.backend.dto.CreateReviewRequest;
import com.food_delivery.backend.dto.RatingSummaryResponse;
import com.food_delivery.backend.dto.ReviewResponse;
import com.food_delivery.backend.entity.*;
import com.food_delivery.backend.exception.BadRequestException;
import com.food_delivery.backend.exception.ConflictException;
import com.food_delivery.backend.exception.ResourceNotFoundException;
import com.food_delivery.backend.repository.MenuItemRepository;
import com.food_delivery.backend.repository.OrderRepository;
import com.food_delivery.backend.repository.RestaurantRepository;
import com.food_delivery.backend.repository.ReviewRepository;
import com.food_delivery.backend.repository.UserRepository;
import com.food_delivery.backend.service.ReviewService;
import com.food_delivery.backend.util.PagingUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final RestaurantRepository restaurantRepository;
    private final MenuItemRepository menuItemRepository;

    @Override
    @Transactional
    public ReviewResponse submitReview(Long userId, CreateReviewRequest request) {

        // 1. Order must exist and belong to this user
        Order order = orderRepository.findByIdAndUserId(request.getOrderId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        // 2. Order must be DELIVERED — can't review before delivery is complete
        if (order.getOrderStatus() != OrderStatus.DELIVERED) {
            throw new BadRequestException("You can only review after your order has been delivered");
        }

        // 3. Validate the target actually exists and is related to this order
        validateTarget(request, order);

        // 4. Prevent duplicate review for the same order + target
        if (reviewRepository.existsByUserIdAndReviewTypeAndTargetIdAndOrderId(
                userId, request.getReviewType(), request.getTargetId(), request.getOrderId())) {
            throw new ConflictException("You have already reviewed this for the selected order");
        }

        Review review = Review.builder()
                .userId(userId)
                .orderId(request.getOrderId())
                .reviewType(request.getReviewType())
                .targetId(request.getTargetId())
                .rating(request.getRating())
                .comment(request.getComment())
                .build();

        Review saved = reviewRepository.save(review);
        log.info("Review submitted by user {} for {} id {}", userId, request.getReviewType(), request.getTargetId());
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getReviewsForTarget(ReviewType reviewType, Long targetId, int page, int size) {
        Pageable pageable = reviewsPageable(page, size);
        return reviewRepository.findByReviewTypeAndTargetId(reviewType, targetId, pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getReviewsByUser(Long userId, int page, int size) {
        Pageable pageable = reviewsPageable(page, size);
        return reviewRepository.findByUserId(userId, pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public RatingSummaryResponse getRatingSummary(ReviewType reviewType, Long targetId) {
        double avg = reviewRepository.findAverageRating(reviewType, targetId)
                .orElse(0.0);
        long total = reviewRepository.countByReviewTypeAndTargetId(reviewType, targetId);

        // Round to 1 decimal place
        double rounded = Math.round(avg * 10.0) / 10.0;

        return RatingSummaryResponse.builder()
                .targetId(targetId)
                .targetType(reviewType.name())
                .averageRating(rounded)
                .totalReviews(total)
                .build();
    }

    @Override
    @Transactional
    public void deleteReview(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));
        reviewRepository.delete(review);
        log.info("Review {} deleted by admin", reviewId);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Validates that the target being reviewed is actually linked to the order.
     * For example, you cannot review a restaurant that was not part of your order.
     */
    private void validateTarget(CreateReviewRequest request, Order order) {
        switch (request.getReviewType()) {

            case RESTAURANT -> {
                // The restaurant being reviewed must be the one the order was placed at
                if (!order.getRestaurantId().equals(request.getTargetId())) {
                    throw new BadRequestException("This restaurant was not part of your order");
                }
                restaurantRepository.findById(request.getTargetId())
                        .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found"));
            }

            case MENU_ITEM -> {
                // The menu item must have been in this order
                boolean itemInOrder = order.getItems().stream()
                        .anyMatch(item -> item.getMenuItemId().equals(request.getTargetId()));
                if (!itemInOrder) {
                    throw new BadRequestException("This item was not part of your order");
                }
                menuItemRepository.findById(request.getTargetId())
                        .orElseThrow(() -> new ResourceNotFoundException("Menu item not found"));
            }

            case DELIVERY_AGENT -> {
                // The delivery agent must be the one who delivered this order
                if (order.getDeliveryAgentId() == null) {
                    throw new BadRequestException("This order had no delivery agent");
                }
                if (!order.getDeliveryAgentId().equals(request.getTargetId())) {
                    throw new BadRequestException("This delivery agent was not assigned to your order");
                }
                User agent = userRepository.findByIdAndDeletedAtIsNull(request.getTargetId())
                        .orElseThrow(() -> new ResourceNotFoundException("Delivery agent not found"));
                if (agent.getRole() != Role.DELIVERY_AGENT) {
                    throw new BadRequestException("Target user is not a delivery agent");
                }
            }

            default -> throw new BadRequestException("Unsupported review type: " + request.getReviewType());
        }
    }

    private Pageable reviewsPageable(int page, int size) {
        return PagingUtils.pageRequest(page, size, Sort.by(Sort.Direction.DESC, "createdAt", "id"));
    }

    private ReviewResponse toResponse(Review review) {
        return ReviewResponse.builder()
                .id(review.getId())
                .userId(review.getUserId())
                .orderId(review.getOrderId())
                .reviewType(review.getReviewType())
                .targetId(review.getTargetId())
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }
}