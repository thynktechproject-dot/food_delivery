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

        log.info("Submitting review for userId: {}, orderId: {}", userId, request.getOrderId());

        Order order = orderRepository.findByIdAndUserId(request.getOrderId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        log.debug("Order fetched with status: {}", order.getOrderStatus());

        if (order.getOrderStatus() != OrderStatus.DELIVERED) {
            log.warn("Review attempted before delivery completion for orderId: {}", request.getOrderId());
            throw new BadRequestException("You can only review after your order has been delivered");
        }

        validateTarget(request, order);

        log.debug("Target validated for reviewType: {}, targetId: {}", request.getReviewType(), request.getTargetId());

        if (reviewRepository.existsByUserIdAndReviewTypeAndTargetIdAndOrderId(
                userId, request.getReviewType(), request.getTargetId(), request.getOrderId())) {
            log.warn("Duplicate review attempt by userId: {}, orderId: {}", userId, request.getOrderId());
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

        log.info("Fetching reviews for targetType: {}, targetId: {}, page: {}, size: {}", reviewType, targetId, page, size);

        Pageable pageable = reviewsPageable(page, size);

        return reviewRepository.findByReviewTypeAndTargetId(reviewType, targetId, pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getReviewsByUser(Long userId, int page, int size) {

        log.info("Fetching reviews by userId: {}, page: {}, size: {}", userId, page, size);

        Pageable pageable = reviewsPageable(page, size);

        return reviewRepository.findByUserId(userId, pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public RatingSummaryResponse getRatingSummary(ReviewType reviewType, Long targetId) {

        log.info("Fetching rating summary for targetType: {}, targetId: {}", reviewType, targetId);

        double avg = reviewRepository.findAverageRating(reviewType, targetId)
                .orElse(0.0);

        long total = reviewRepository.countByReviewTypeAndTargetId(reviewType, targetId);

        double rounded = Math.round(avg * 10.0) / 10.0;

        log.debug("Rating summary calculated - avg: {}, rounded: {}, totalReviews: {}", avg, rounded, total);

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

        log.info("Deleting review with id: {}", reviewId);

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));

        reviewRepository.delete(review);

        log.info("Review {} deleted by admin", reviewId);
    }

    private void validateTarget(CreateReviewRequest request, Order order) {

        log.debug("Validating target for reviewType: {}, targetId: {}", request.getReviewType(), request.getTargetId());

        switch (request.getReviewType()) {

            case RESTAURANT -> {
                if (!order.getRestaurantId().equals(request.getTargetId())) {
                    log.warn("Invalid restaurant review attempt for orderId: {}", order.getId());
                    throw new BadRequestException("This restaurant was not part of your order");
                }
                restaurantRepository.findById(request.getTargetId())
                        .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found"));
            }

            case MENU_ITEM -> {
                boolean itemInOrder = order.getItems().stream()
                        .anyMatch(item -> item.getMenuItemId().equals(request.getTargetId()));

                if (!itemInOrder) {
                    log.warn("Invalid menu item review attempt for orderId: {}", order.getId());
                    throw new BadRequestException("This item was not part of your order");
                }

                menuItemRepository.findById(request.getTargetId())
                        .orElseThrow(() -> new ResourceNotFoundException("Menu item not found"));
            }

            case DELIVERY_AGENT -> {
                if (order.getDeliveryAgentId() == null) {
                    log.warn("Review attempted for order without delivery agent, orderId: {}", order.getId());
                    throw new BadRequestException("This order had no delivery agent");
                }

                if (!order.getDeliveryAgentId().equals(request.getTargetId())) {
                    log.warn("Invalid delivery agent review attempt for orderId: {}", order.getId());
                    throw new BadRequestException("This delivery agent was not assigned to your order");
                }

                User agent = userRepository.findByIdAndDeletedAtIsNull(request.getTargetId())
                        .orElseThrow(() -> new ResourceNotFoundException("Delivery agent not found"));

                if (agent.getRole() != Role.DELIVERY_AGENT) {
                    log.warn("Invalid role for delivery agent review, userId: {}", request.getTargetId());
                    throw new BadRequestException("Target user is not a delivery agent");
                }
            }

            default -> {
                log.warn("Unsupported review type: {}", request.getReviewType());
                throw new BadRequestException("Unsupported review type: " + request.getReviewType());
            }
        }
    }

    private Pageable reviewsPageable(int page, int size) {
        log.debug("Creating pageable for page: {}, size: {}", page, size);
        return PagingUtils.pageRequest(page, size, Sort.by(Sort.Direction.DESC, "createdAt", "id"));
    }

    private ReviewResponse toResponse(Review review) {
        log.debug("Mapping Review to ReviewResponse for reviewId: {}", review.getId());
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