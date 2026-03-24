package com.food_delivery.backend.dto;

import com.food_delivery.backend.entity.ReviewType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ReviewResponse {

    private Long id;
    private Long userId;
    private Long orderId;
    private ReviewType reviewType;
    private Long targetId;
    private int rating;
    private String comment;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}