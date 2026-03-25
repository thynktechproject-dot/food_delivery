package com.food_delivery.backend.service.impl;

import com.food_delivery.backend.dto.MonthlySummaryResponse;
import com.food_delivery.backend.dto.TodaySummaryResponse;
import com.food_delivery.backend.dto.WeeklySummaryResponse;
import com.food_delivery.backend.mapper.DashboardMapper;
import com.food_delivery.backend.repository.OrderRepository;
import com.food_delivery.backend.repository.ReviewRepository;
import com.food_delivery.backend.service.OwnerDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Service
@RequiredArgsConstructor
public class OwnerDashboardServiceImpl implements OwnerDashboardService {

    private final OrderRepository orderRepository;
    private final ReviewRepository reviewRepository;

    @Override
    public TodaySummaryResponse getTodaySummary(Long restaurantId) {

        LocalDateTime startOfDay = LocalDateTime.now().with(LocalTime.MIN);
        LocalDateTime endOfDay = LocalDateTime.now().with(LocalTime.MAX);

        Double totalSales = orderRepository.getTodayTotalSales(
                restaurantId, startOfDay, endOfDay
        );

        Long totalOrders = orderRepository.getTodayTotalOrders(
                restaurantId, startOfDay, endOfDay
        );

        Double avgRating = reviewRepository.getTodayAverageRating(
                restaurantId, startOfDay, endOfDay
        );

        return DashboardMapper.toTodaySummaryResponse(
                totalSales,
                totalOrders,
                avgRating
        );
    }
    
    @Override
    public WeeklySummaryResponse getWeeklySummary(Long restaurantId) {

        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusDays(6).with(LocalTime.MIN);

        Double totalRevenue = orderRepository.getRevenueBetween(
                restaurantId, startDate, endDate
        );

        Long totalOrders = orderRepository.getOrdersBetween(
                restaurantId, startDate, endDate
        );

        Double peakRevenue = orderRepository.getPeakDailyRevenue(
                restaurantId, startDate, endDate
        );

        return DashboardMapper.toWeeklySummaryResponse(
                totalRevenue,
                totalOrders,
                peakRevenue
        );
    }
    
    @Override
    public MonthlySummaryResponse getMonthlySummary(Long restaurantId) {

        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusDays(29).with(LocalTime.MIN);

        Double totalRevenue = orderRepository.getRevenueBetween(
                restaurantId, startDate, endDate
        );

        Long totalOrders = orderRepository.getOrdersBetween(
                restaurantId, startDate, endDate
        );

        Double peakRevenue = orderRepository.getPeakDailyRevenue(
                restaurantId, startDate, endDate
        );

        return DashboardMapper.toMonthlySummaryResponse(
                totalRevenue,
                totalOrders,
                peakRevenue
        );
    }
}