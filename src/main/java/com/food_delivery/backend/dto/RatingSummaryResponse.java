package com.food_delivery.backend.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RatingSummaryResponse {

    private Long targetId;
    private String targetType;
    private double averageRating;
    private long totalReviews;
}