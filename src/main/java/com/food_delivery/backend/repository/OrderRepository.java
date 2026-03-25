package com.food_delivery.backend.repository;

import com.food_delivery.backend.entity.Order;
import com.food_delivery.backend.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

	@Override
	@EntityGraph(attributePaths = "items")
	List<Order> findAll();

	@Override
	@EntityGraph(attributePaths = "items")
	Optional<Order> findById(Long id);

	Page<Order> findAllBy(Pageable pageable);

	@EntityGraph(attributePaths = "items")
	List<Order> findByUserId(Long userId);

	Page<Order> findByUserId(Long userId, Pageable pageable);

	@EntityGraph(attributePaths = "items")
	Optional<Order> findByIdAndUserId(Long id, Long userId);

	@EntityGraph(attributePaths = "items")
	List<Order> findByRestaurantId(Long restaurantId);

	Page<Order> findByRestaurantId(Long restaurantId, Pageable pageable);

	@EntityGraph(attributePaths = "items")
	Optional<Order> findByIdAndRestaurantId(Long id, Long restaurantId);

	@EntityGraph(attributePaths = "items")
	List<Order> findByDeliveryAgentId(Long deliveryAgentId);

	Page<Order> findByDeliveryAgentId(Long deliveryAgentId, Pageable pageable);

	@EntityGraph(attributePaths = "items")
	Optional<Order> findByIdAndDeliveryAgentId(Long id, Long deliveryAgentId);

	// Added: filter assigned orders by a set of statuses — used for active/history
	// views
	Page<Order> findByDeliveryAgentIdAndOrderStatusIn(Long deliveryAgentId, Collection<OrderStatus> statuses,
			Pageable pageable);

	long countByOrderStatus(OrderStatus orderStatus);

	long countByDeliveryAgentIdAndOrderStatusIn(Long deliveryAgentId, Collection<OrderStatus> statuses);

	@Query("""
			    SELECT COALESCE(SUM(o.totalAmount), 0)
			    FROM Order o
			    WHERE o.restaurantId = :restaurantId
			    AND o.orderStatus = 'DELIVERED'
			    AND o.createdAt >= :startOfDay
			    AND o.createdAt < :endOfDay
			""")
	Double getTodayTotalSales(Long restaurantId, LocalDateTime startOfDay, LocalDateTime endOfDay);

	@Query("""
			    SELECT COUNT(o)
			    FROM Order o
			    WHERE o.restaurantId = :restaurantId
			    AND o.orderStatus = 'DELIVERED'
			    AND o.createdAt >= :startOfDay
			    AND o.createdAt < :endOfDay
			""")
	Long getTodayTotalOrders(Long restaurantId, LocalDateTime startOfDay, LocalDateTime endOfDay);

	@Query("""
			    SELECT COALESCE(SUM(o.totalAmount), 0)
			    FROM Order o
			    WHERE o.restaurantId = :restaurantId
			    AND o.orderStatus = 'DELIVERED'
			    AND o.createdAt BETWEEN :startDate AND :endDate
			""")
	Double getRevenueBetween(Long restaurantId, LocalDateTime startDate, LocalDateTime endDate);

	@Query("""
			    SELECT COUNT(o)
			    FROM Order o
			    WHERE o.restaurantId = :restaurantId
			    AND o.orderStatus = 'DELIVERED'
			    AND o.createdAt BETWEEN :startDate AND :endDate
			""")
	Long getOrdersBetween(Long restaurantId, LocalDateTime startDate, LocalDateTime endDate);

	@Query("""
			    SELECT COALESCE(MAX(dailyTotal), 0)
			    FROM (
			        SELECT SUM(o.totalAmount) as dailyTotal
			        FROM Order o
			        WHERE o.restaurantId = :restaurantId
			        AND o.orderStatus = 'DELIVERED'
			        AND o.createdAt BETWEEN :startDate AND :endDate
			        GROUP BY FUNCTION('DATE', o.createdAt)
			    )
			""")
	Double getPeakDailyRevenue(Long restaurantId, LocalDateTime startDate, LocalDateTime endDate);

	List<Order> findByRestaurantIdAndOrderStatusIn(Long restaurantId, List<OrderStatus> statuses);

	Page<Order> findByRestaurantIdAndOrderStatusIn(Long restaurantId, List<OrderStatus> statuses, Pageable pageable);
}