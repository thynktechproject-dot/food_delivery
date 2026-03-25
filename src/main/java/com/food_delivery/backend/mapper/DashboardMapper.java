package com.food_delivery.backend.mapper;

import com.food_delivery.backend.dto.MonthlySummaryResponse;
import com.food_delivery.backend.dto.TodaySummaryResponse;
import com.food_delivery.backend.dto.WeeklySummaryResponse;

public class DashboardMapper {

	public static TodaySummaryResponse toTodaySummaryResponse(Double totalSales, Long totalOrders, Double avgRating) {

		Double avgOrderValue = totalOrders > 0 ? totalSales / totalOrders : 0;

		return TodaySummaryResponse.builder().totalSales(totalSales).totalOrders(totalOrders).avgRating(avgRating)
				.avgOrderValue(avgOrderValue).build();
	}

	public static WeeklySummaryResponse toWeeklySummaryResponse(Double totalRevenue, Long totalOrders,
			Double peakRevenue) {

		Double avgDailyRevenue = totalRevenue / 7;

		return WeeklySummaryResponse.builder().totalRevenue(totalRevenue).totalOrders(totalOrders)
				.avgDailyRevenue(avgDailyRevenue).peakRevenue(peakRevenue).build();
	}
	
	public static MonthlySummaryResponse toMonthlySummaryResponse(
	        Double totalRevenue,
	        Long totalOrders,
	        Double peakRevenue
	) {

	    Double avgDailyRevenue = totalRevenue / 30;

	    return MonthlySummaryResponse.builder()
	            .totalRevenue(totalRevenue)
	            .totalOrders(totalOrders)
	            .avgDailyRevenue(avgDailyRevenue)
	            .peakRevenue(peakRevenue)
	            .build();
	}
}