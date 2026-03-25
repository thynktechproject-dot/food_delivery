package com.food_delivery.backend.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TodaySummaryResponse {

    private Double totalSales;
    private Long totalOrders;
    private Double avgRating;
    private Double avgOrderValue;
}