package com.food_delivery.backend.dto;

import com.food_delivery.backend.entity.ReviewType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateReviewRequest {

    /**
     * The order this review is based on. Must be a DELIVERED order
     * belonging to the calling user.
     */
    @NotNull(message = "orderId is required")
    private Long orderId;

    /**
     * What you are reviewing: RESTAURANT, MENU_ITEM, or DELIVERY_AGENT.
     */
    @NotNull(message = "reviewType is required")
    private ReviewType reviewType;

    /**
     * ID of the entity being reviewed.
     * - RESTAURANT   → restaurant ID
     * - MENU_ITEM    → menu item ID
     * - DELIVERY_AGENT → user ID of the delivery agent
     */
    @NotNull(message = "targetId is required")
    private Long targetId;

    /** Star rating between 1 and 5. */
    @NotNull(message = "rating is required")
    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating must be at most 5")
    private Integer rating;

    /** Optional text feedback (max 1000 characters). */
    @Size(max = 1000, message = "Comment must not exceed 1000 characters")
    private String comment;
}