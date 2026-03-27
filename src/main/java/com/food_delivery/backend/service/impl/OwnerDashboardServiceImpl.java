package com.food_delivery.backend.service.impl;

import com.food_delivery.backend.dto.MonthlySummaryResponse;
import com.food_delivery.backend.dto.TodaySummaryResponse;
import com.food_delivery.backend.dto.WeeklySummaryResponse;
import com.food_delivery.backend.mapper.DashboardMapper;
import com.food_delivery.backend.repository.OrderRepository;
import com.food_delivery.backend.repository.ReviewRepository;
import com.food_delivery.backend.service.OwnerDashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class OwnerDashboardServiceImpl implements OwnerDashboardService {

    private final OrderRepository orderRepository;
    private final ReviewRepository reviewRepository;

    @Override
    public TodaySummaryResponse getTodaySummary(Long restaurantId) {

        log.info("Fetching today summary for restaurantId: {}", restaurantId);

        LocalDateTime startOfDay = LocalDateTime.now().with(LocalTime.MIN);
        LocalDateTime endOfDay = LocalDateTime.now().with(LocalTime.MAX);

        log.debug("Today summary time range - start: {}, end: {}", startOfDay, endOfDay);

        Double totalSales = orderRepository.getTodayTotalSales(
                restaurantId, startOfDay, endOfDay
        );

        Long totalOrders = orderRepository.getTodayTotalOrders(
                restaurantId, startOfDay, endOfDay
        );

        Double avgRating = reviewRepository.getTodayAverageRating(
                restaurantId, startOfDay, endOfDay
        );

        log.debug("Today summary data - totalSales: {}, totalOrders: {}, avgRating: {}",
                totalSales, totalOrders, avgRating);

        TodaySummaryResponse response = DashboardMapper.toTodaySummaryResponse(
                totalSales,
                totalOrders,
                avgRating
        );

        log.info("Successfully fetched today summary for restaurantId: {}", restaurantId);

        return response;
    }
    
    @Override
    public WeeklySummaryResponse getWeeklySummary(Long restaurantId) {

        log.info("Fetching weekly summary for restaurantId: {}", restaurantId);

        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusDays(6).with(LocalTime.MIN);

        log.debug("Weekly summary time range - start: {}, end: {}", startDate, endDate);

        Double totalRevenue = orderRepository.getRevenueBetween(
                restaurantId, startDate, endDate
        );

        Long totalOrders = orderRepository.getOrdersBetween(
                restaurantId, startDate, endDate
        );

        Double peakRevenue = orderRepository.getPeakDailyRevenue(
                restaurantId, startDate, endDate
        );

        log.debug("Weekly summary data - totalRevenue: {}, totalOrders: {}, peakRevenue: {}",
                totalRevenue, totalOrders, peakRevenue);

        WeeklySummaryResponse response = DashboardMapper.toWeeklySummaryResponse(
                totalRevenue,
                totalOrders,
                peakRevenue
        );

        log.info("Successfully fetched weekly summary for restaurantId: {}", restaurantId);

        return response;
    }
    
    @Override
    public MonthlySummaryResponse getMonthlySummary(Long restaurantId) {

        log.info("Fetching monthly summary for restaurantId: {}", restaurantId);

        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusDays(29).with(LocalTime.MIN);

        log.debug("Monthly summary time range - start: {}, end: {}", startDate, endDate);

        Double totalRevenue = orderRepository.getRevenueBetween(
                restaurantId, startDate, endDate
        );

        Long totalOrders = orderRepository.getOrdersBetween(
                restaurantId, startDate, endDate
        );

        Double peakRevenue = orderRepository.getPeakDailyRevenue(
                restaurantId, startDate, endDate
        );

        log.debug("Monthly summary data - totalRevenue: {}, totalOrders: {}, peakRevenue: {}",
                totalRevenue, totalOrders, peakRevenue);

        MonthlySummaryResponse response = DashboardMapper.toMonthlySummaryResponse(
                totalRevenue,
                totalOrders,
                peakRevenue
        );

        log.info("Successfully fetched monthly summary for restaurantId: {}", restaurantId);

        return response;
    }
}