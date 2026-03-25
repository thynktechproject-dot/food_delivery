package com.food_delivery.backend.service;

import com.food_delivery.backend.dto.TodaySummaryResponse;
import com.food_delivery.backend.dto.WeeklySummaryResponse;
import com.food_delivery.backend.dto.MonthlySummaryResponse;

public interface OwnerDashboardService {

    TodaySummaryResponse getTodaySummary(Long restaurantId);
    
    WeeklySummaryResponse getWeeklySummary(Long restaurantId);

    MonthlySummaryResponse getMonthlySummary(Long restaurantId);
}