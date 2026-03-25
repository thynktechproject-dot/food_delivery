package com.food_delivery.backend.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MonthlySummaryResponse {

    private Double totalRevenue;
    private Long totalOrders;
    private Double avgDailyRevenue;
    private Double peakRevenue;
}