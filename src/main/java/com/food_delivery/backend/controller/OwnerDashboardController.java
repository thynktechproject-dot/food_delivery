package com.food_delivery.backend.controller;

import com.food_delivery.backend.dto.ApiResponse;
import com.food_delivery.backend.dto.MonthlySummaryResponse;
import com.food_delivery.backend.dto.TodaySummaryResponse;
import com.food_delivery.backend.dto.WeeklySummaryResponse;
import com.food_delivery.backend.service.OwnerDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/owner/dashboard")
@RequiredArgsConstructor
public class OwnerDashboardController {

    private final OwnerDashboardService dashboardService;

    @GetMapping("/today/{restaurantId}")
    public ResponseEntity<ApiResponse<TodaySummaryResponse>> getTodaySummary(
            @PathVariable Long restaurantId
    ) {

        TodaySummaryResponse data = dashboardService.getTodaySummary(restaurantId);

        ApiResponse<TodaySummaryResponse> response = ApiResponse.<TodaySummaryResponse>builder()
                .success(true)
                .message("Today's summary fetched successfully")
                .data(data)
                .build();

        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/weekly/{restaurantId}")
    public ResponseEntity<ApiResponse<WeeklySummaryResponse>> getWeeklySummary(
            @PathVariable Long restaurantId
    ) {

        WeeklySummaryResponse data = dashboardService.getWeeklySummary(restaurantId);

        ApiResponse<WeeklySummaryResponse> response = ApiResponse.<WeeklySummaryResponse>builder()
                .success(true)
                .message("Weekly summary fetched successfully")
                .data(data)
                .build();

        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/monthly/{restaurantId}")
    public ResponseEntity<ApiResponse<MonthlySummaryResponse>> getMonthlySummary(
            @PathVariable Long restaurantId
    ) {

        MonthlySummaryResponse data = dashboardService.getMonthlySummary(restaurantId);

        ApiResponse<MonthlySummaryResponse> response = ApiResponse.<MonthlySummaryResponse>builder()
                .success(true)
                .message("Monthly summary fetched successfully")
                .data(data)
                .build();

        return ResponseEntity.ok(response);
    }
}