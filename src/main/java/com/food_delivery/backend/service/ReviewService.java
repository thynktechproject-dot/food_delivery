package com.food_delivery.backend.service;

import com.food_delivery.backend.dto.CreateReviewRequest;
import com.food_delivery.backend.dto.RatingSummaryResponse;
import com.food_delivery.backend.dto.ReviewResponse;
import com.food_delivery.backend.entity.ReviewType;
import org.springframework.data.domain.Page;

public interface ReviewService {

    /** Submit a new review. Only callable by the USER who owns the DELIVERED order. */
    ReviewResponse submitReview(Long userId, CreateReviewRequest request);

    /** Get paginated reviews for any target (restaurant, menu item, or agent). */
    Page<ReviewResponse> getReviewsForTarget(ReviewType reviewType, Long targetId, int page, int size);

    /** Get all reviews written by a specific user. */
    Page<ReviewResponse> getReviewsByUser(Long userId, int page, int size);

    /** Average star rating + total count for any target. */
    RatingSummaryResponse getRatingSummary(ReviewType reviewType, Long targetId);

    /** Delete a review — admin only. */
    void deleteReview(Long reviewId);
}