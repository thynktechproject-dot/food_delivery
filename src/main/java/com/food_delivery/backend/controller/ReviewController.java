package com.food_delivery.backend.controller;

import com.food_delivery.backend.dto.ApiResponse;
import com.food_delivery.backend.dto.CreateReviewRequest;
import com.food_delivery.backend.dto.RatingSummaryResponse;
import com.food_delivery.backend.dto.ReviewResponse;
import com.food_delivery.backend.entity.ReviewType;
import com.food_delivery.backend.service.ReviewService;
import com.food_delivery.backend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
@Slf4j
public class ReviewController {

    private final ReviewService reviewService;
    private final UserService userService;

    // -------------------------------------------------------------------------
    // User endpoints — submit and view own reviews
    // -------------------------------------------------------------------------

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ApiResponse<ReviewResponse> submitReview(
            @Valid @RequestBody CreateReviewRequest request,
            Authentication authentication
    ) {
        log.info("POST /api/reviews called by user: {}", authentication.getName());
        log.debug("Submitting review for orderId: {}, targetId: {}, type: {}", 
                request.getOrderId(), request.getTargetId(), request.getReviewType());

        Long userId = resolveUserId(authentication);

        return ApiResponse.<ReviewResponse>builder()
                .success(true)
                .message("Review submitted successfully")
                .data(reviewService.submitReview(userId, request))
                .build();
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('USER')")
    public ApiResponse<Page<ReviewResponse>> getMyReviews(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        log.info("GET /api/reviews/my called by user: {}", authentication.getName());

        Long userId = resolveUserId(authentication);
        log.debug("Fetching reviews for userId: {}, page: {}, size: {}", userId, page, size);

        return ApiResponse.<Page<ReviewResponse>>builder()
                .success(true)
                .message("Your reviews fetched")
                .data(reviewService.getReviewsByUser(userId, page, size))
                .build();
    }

    // -------------------------------------------------------------------------
    // Public endpoints — anyone can read reviews and ratings
    // -------------------------------------------------------------------------

    @GetMapping
    public ApiResponse<Page<ReviewResponse>> getReviews(
            @RequestParam ReviewType reviewType,
            @RequestParam Long targetId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        log.info("GET /api/reviews called");
        log.debug("Fetching reviews for type: {}, targetId: {}, page: {}, size: {}", 
                reviewType, targetId, page, size);

        return ApiResponse.<Page<ReviewResponse>>builder()
                .success(true)
                .message("Reviews fetched")
                .data(reviewService.getReviewsForTarget(reviewType, targetId, page, size))
                .build();
    }

    @GetMapping("/summary")
    public ApiResponse<RatingSummaryResponse> getRatingSummary(
            @RequestParam ReviewType reviewType,
            @RequestParam Long targetId
    ) {
        log.info("GET /api/reviews/summary called");
        log.debug("Fetching rating summary for type: {}, targetId: {}", reviewType, targetId);

        return ApiResponse.<RatingSummaryResponse>builder()
                .success(true)
                .message("Rating summary fetched")
                .data(reviewService.getRatingSummary(reviewType, targetId))
                .build();
    }

    // -------------------------------------------------------------------------
    // Admin endpoint — delete a review
    // -------------------------------------------------------------------------

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteReview(@PathVariable Long id) {
        log.info("DELETE /api/reviews/{} called", id);
        log.debug("Deleting review with id: {}", id);

        reviewService.deleteReview(id);

        log.debug("Review deleted successfully for id: {}", id);

        return ApiResponse.<Void>builder()
                .success(true)
                .message("Review deleted")
                .data(null)
                .build();
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private Long resolveUserId(Authentication authentication) {
        log.debug("Resolving userId for email: {}", authentication.getName());
        return userService.getUserByEmail(authentication.getName()).getId();
    }
}