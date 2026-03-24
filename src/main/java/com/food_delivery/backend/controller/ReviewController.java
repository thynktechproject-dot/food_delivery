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

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;
    private final UserService userService;

    // -------------------------------------------------------------------------
    // User endpoints — submit and view own reviews
    // -------------------------------------------------------------------------

    /**
     * Submit a review for a RESTAURANT, MENU_ITEM, or DELIVERY_AGENT.
     * Only allowed after the order is DELIVERED.
     *
     * POST /api/reviews
     * Body: { orderId, reviewType, targetId, rating, comment }
     */
    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ApiResponse<ReviewResponse> submitReview(
            @Valid @RequestBody CreateReviewRequest request,
            Authentication authentication
    ) {
        Long userId = resolveUserId(authentication);
        return ApiResponse.<ReviewResponse>builder()
                .success(true)
                .message("Review submitted successfully")
                .data(reviewService.submitReview(userId, request))
                .build();
    }

    /**
     * View all reviews written by the logged-in user.
     *
     * GET /api/reviews/my
     */
    @GetMapping("/my")
    @PreAuthorize("hasRole('USER')")
    public ApiResponse<Page<ReviewResponse>> getMyReviews(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Long userId = resolveUserId(authentication);
        return ApiResponse.<Page<ReviewResponse>>builder()
                .success(true)
                .message("Your reviews fetched")
                .data(reviewService.getReviewsByUser(userId, page, size))
                .build();
    }

    // -------------------------------------------------------------------------
    // Public endpoints — anyone can read reviews and ratings
    // -------------------------------------------------------------------------

    /**
     * Get paginated reviews for a target.
     *
     * GET /api/reviews?reviewType=RESTAURANT&targetId=3
     * GET /api/reviews?reviewType=MENU_ITEM&targetId=12
     * GET /api/reviews?reviewType=DELIVERY_AGENT&targetId=7
     */
    @GetMapping
    public ApiResponse<Page<ReviewResponse>> getReviews(
            @RequestParam ReviewType reviewType,
            @RequestParam Long targetId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.<Page<ReviewResponse>>builder()
                .success(true)
                .message("Reviews fetched")
                .data(reviewService.getReviewsForTarget(reviewType, targetId, page, size))
                .build();
    }

    /**
     * Get the average star rating and total review count for a target.
     *
     * GET /api/reviews/summary?reviewType=RESTAURANT&targetId=3
     */
    @GetMapping("/summary")
    public ApiResponse<RatingSummaryResponse> getRatingSummary(
            @RequestParam ReviewType reviewType,
            @RequestParam Long targetId
    ) {
        return ApiResponse.<RatingSummaryResponse>builder()
                .success(true)
                .message("Rating summary fetched")
                .data(reviewService.getRatingSummary(reviewType, targetId))
                .build();
    }

    // -------------------------------------------------------------------------
    // Admin endpoint — delete a review
    // -------------------------------------------------------------------------

    /**
     * Admin can delete any review (e.g. spam or abusive content).
     *
     * DELETE /api/reviews/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteReview(@PathVariable Long id) {
        reviewService.deleteReview(id);
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
        return userService.getUserByEmail(authentication.getName()).getId();
    }
}