package com.food_delivery.backend.service.impl;

import com.food_delivery.backend.dto.DeliveryStatsResponse;
import com.food_delivery.backend.dto.OrderResponse;
import com.food_delivery.backend.dto.OrderItemResponse;
import com.food_delivery.backend.entity.*;
import com.food_delivery.backend.enums.RestaurantStatus;
import com.food_delivery.backend.exception.BadRequestException;
import com.food_delivery.backend.exception.ForbiddenException;
import com.food_delivery.backend.exception.ResourceNotFoundException;
import com.food_delivery.backend.repository.CartRepository;
import com.food_delivery.backend.repository.OrderRepository;
import com.food_delivery.backend.repository.RestaurantRepository;
import com.food_delivery.backend.repository.UserRepository;
import com.food_delivery.backend.service.OrderService;
import com.food_delivery.backend.util.PagingUtils;
import com.food_delivery.backend.notification.NotificationService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

	private final CartRepository cartRepository;
	private final OrderRepository orderRepository;
	private final UserRepository userRepository;
	private final RestaurantRepository restaurantRepository;
	private final NotificationService notificationService;

	/** Statuses that count as an "active" delivery in progress. */
	private static final Set<OrderStatus> ACTIVE_DELIVERY_STATUSES = Set.of(OrderStatus.PREPARING,
			OrderStatus.OUT_FOR_DELIVERY);

	/** Statuses that appear in delivery history. */
	private static final Set<OrderStatus> HISTORY_STATUSES = Set.of(OrderStatus.DELIVERED);

	@Override
	@Transactional
	public OrderResponse placeOrder(Long userId) {
		Cart cart = cartRepository.findByUserId(userId)
				.orElseThrow(() -> new ResourceNotFoundException("Cart not found"));

		if (cart.getItems().isEmpty()) {
			throw new BadRequestException("Cart is empty");
		}

		restaurantRepository.findById(cart.getRestaurantId())
				.filter(restaurant -> restaurant.getStatus() == RestaurantStatus.APPROVED && restaurant.isActive())
				.orElseThrow(() -> new BadRequestException("Orders can only be placed for available restaurants"));

		Order order = Order.builder().userId(cart.getUserId()).restaurantId(cart.getRestaurantId())
				.orderStatus(OrderStatus.CREATED).build();

		List<OrderItem> orderItems = cart
				.getItems().stream().map(item -> OrderItem.builder().menuItemId(item.getMenuItemId())
						.name(item.getName()).price(item.getPrice()).quantity(item.getQuantity()).order(order).build())
				.toList();

		double total = orderItems.stream().mapToDouble(i -> i.getPrice() * i.getQuantity()).sum();

		order.setItems(orderItems);
		order.setTotalAmount(total);

		Order savedOrder = orderRepository.save(order);

		cart.getItems().clear();
		cart.setRestaurantId(null);
		cartRepository.save(cart);

	// (Notifications for user and restaurant owner are now sent after payment)

		return toResponse(savedOrder);
	}

	@Override
	public OrderResponse getOrderById(Long orderId) {
		return toResponse(getOrderOrThrow(orderId));
	}

	@Override
	public OrderResponse getOrderByUser(Long orderId, Long userId) {
		Order order = orderRepository.findByIdAndUserId(orderId, userId)
				.orElseThrow(() -> new ResourceNotFoundException("Order not found"));
		return toResponse(order);
	}

	@Override
	public OrderResponse getOrderByRestaurantOwner(Long orderId, Long ownerId) {
		Long restaurantId = getOwnerRestaurantId(ownerId);
		Order order = orderRepository.findByIdAndRestaurantId(orderId, restaurantId)
				.orElseThrow(() -> new ResourceNotFoundException("Order not found"));
		return toResponse(order);
	}

	@Override
	public OrderResponse getOrderByDeliveryAgent(Long orderId, Long deliveryAgentId) {
		Order order = orderRepository.findByIdAndDeliveryAgentId(orderId, deliveryAgentId)
				.orElseThrow(() -> new ResourceNotFoundException("Order not found"));
		return toResponse(order);
	}

	@Override
	@org.springframework.transaction.annotation.Transactional(readOnly = true)
	public Page<OrderResponse> getOrdersByUserId(Long userId, int page, int size) {
		Pageable pageable = ordersPageable(page, size);
		return orderRepository.findByUserId(userId, pageable).map(this::toResponse);
	}

	@Override
	@org.springframework.transaction.annotation.Transactional(readOnly = true)
	public Page<OrderResponse> getOrdersByRestaurantOwner(Long ownerId, int page, int size) {
		Long restaurantId = getOwnerRestaurantId(ownerId);
		return orderRepository.findByRestaurantId(restaurantId, ordersPageable(page, size)).map(this::toResponse);
	}

	@Override
	@org.springframework.transaction.annotation.Transactional(readOnly = true)
	public Page<OrderResponse> getOrdersByDeliveryAgentId(Long deliveryAgentId, int page, int size) {
		return orderRepository.findByDeliveryAgentId(deliveryAgentId, ordersPageable(page, size)).map(this::toResponse);
	}

	/**
	 * Returns only orders that are currently in progress for the agent (PREPARING
	 * or OUT_FOR_DELIVERY). Useful for the agent's live dashboard.
	 */
	@Override
	@org.springframework.transaction.annotation.Transactional(readOnly = true)
	public Page<OrderResponse> getActiveOrdersByDeliveryAgentId(Long deliveryAgentId, int page, int size) {
		return orderRepository.findByDeliveryAgentIdAndOrderStatusIn(deliveryAgentId, ACTIVE_DELIVERY_STATUSES,
				ordersPageable(page, size)).map(this::toResponse);
	}

	/**
	 * Returns the agent's completed delivery history (DELIVERED orders only),
	 * sorted newest-first.
	 */
	@Override
	@org.springframework.transaction.annotation.Transactional(readOnly = true)
	public Page<OrderResponse> getDeliveryHistoryByAgentId(Long deliveryAgentId, int page, int size) {
		return orderRepository
				.findByDeliveryAgentIdAndOrderStatusIn(deliveryAgentId, HISTORY_STATUSES, ordersPageable(page, size))
				.map(this::toResponse);
	}

	/**
	 * Aggregated stats for a delivery agent's personal dashboard: total assigned,
	 * currently active, and total delivered.
	 */
	@Override
	@org.springframework.transaction.annotation.Transactional(readOnly = true)
	public DeliveryStatsResponse getDeliveryStatsByAgentId(Long deliveryAgentId) {
		long totalAssigned = orderRepository.countByDeliveryAgentIdAndOrderStatusIn(deliveryAgentId,
				Set.of(OrderStatus.values()));
		long activeDeliveries = orderRepository.countByDeliveryAgentIdAndOrderStatusIn(deliveryAgentId,
				ACTIVE_DELIVERY_STATUSES);
		long totalDelivered = orderRepository.countByDeliveryAgentIdAndOrderStatusIn(deliveryAgentId, HISTORY_STATUSES);

		return DeliveryStatsResponse.builder().totalAssigned(totalAssigned).activeDeliveries(activeDeliveries)
				.totalDelivered(totalDelivered).build();
	}

	@Override
	@org.springframework.transaction.annotation.Transactional(readOnly = true)
	public Page<OrderResponse> getAllOrders(int page, int size) {
		return orderRepository.findAllBy(ordersPageable(page, size)).map(this::toResponse);
	}

	@Override
	@Transactional
	public OrderResponse updateStatus(Long orderId, OrderStatus newStatus, String actorEmail) {
		Order order = getOrderOrThrow(orderId);
		User actor = userRepository.findByEmailAndDeletedAtIsNull(actorEmail)
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));

		validateActorCanManageOrder(order, actor, newStatus);
		validateTransition(order, actor, newStatus);

		order.setOrderStatus(newStatus);
		Order savedOrder = orderRepository.save(order);

		// Notify user if delivered
		if (newStatus == OrderStatus.DELIVERED) {
			User user = userRepository.findByIdAndDeletedAtIsNull(order.getUserId()).orElse(null);
			if (user != null) {
				notificationService.notifyUserOrderDelivered(user.getEmail(), "Order ID: " + order.getId());
			}
		}

		return toResponse(savedOrder);
	}

	@Override
	@Transactional
	public void cancelOrderByAdmin(Long orderId) {
		Order order = getOrderOrThrow(orderId);
		if (order.getOrderStatus() == OrderStatus.DELIVERED) {
			throw new BadRequestException("Delivered orders cannot be cancelled");
		}
		order.setOrderStatus(OrderStatus.CANCELLED);
		orderRepository.save(order);
	}

	@Override
	@Transactional
	public OrderResponse assignDeliveryAgent(Long orderId, Long deliveryAgentId) {
		Order order = getOrderOrThrow(orderId);
		User deliveryAgent = userRepository.findByIdAndDeletedAtIsNull(deliveryAgentId)
				.orElseThrow(() -> new ResourceNotFoundException("Delivery agent not found"));

		if (order.getOrderStatus() == OrderStatus.CREATED || order.getOrderStatus() == OrderStatus.PAID
				|| order.getOrderStatus() == OrderStatus.CANCELLED || order.getOrderStatus() == OrderStatus.DELIVERED
				|| order.getOrderStatus() == OrderStatus.FAILED) {
			throw new BadRequestException("Delivery agent can only be assigned after the order reaches preparation");
		}

		if (deliveryAgent.getRole() != Role.DELIVERY_AGENT) {
			throw new BadRequestException("User is not a delivery agent");
		}

		if (!deliveryAgent.isActive()) {
			throw new BadRequestException("Delivery agent must be active before assignment");
		}

		order.setDeliveryAgentId(deliveryAgentId);
		Order savedOrder = orderRepository.save(order);


		// Notify delivery agent with all details
		User user = userRepository.findByIdAndDeletedAtIsNull(order.getUserId()).orElse(null);
		Restaurant restaurant = restaurantRepository.findById(order.getRestaurantId()).orElse(null);
		if (deliveryAgent != null && user != null && restaurant != null) {
			notificationService.notifyDeliveryAgentAssigned(deliveryAgent, order, user, restaurant);
		}

		return toResponse(savedOrder);
	}

	// -------------------------------------------------------------------------
	// Private helpers
	// -------------------------------------------------------------------------

	private Order getOrderOrThrow(Long orderId) {
		return orderRepository.findById(orderId).orElseThrow(() -> new ResourceNotFoundException("Order not found"));
	}

	private Long getOwnerRestaurantId(Long ownerId) {
		return restaurantRepository.findByOwnerIdAndActiveTrue(ownerId).map(Restaurant::getId)
				.orElseThrow(() -> new ResourceNotFoundException("Restaurant not found"));
	}

	private Pageable ordersPageable(int page, int size) {
		return PagingUtils.pageRequest(page, size, Sort.by(Sort.Direction.DESC, "createdAt", "id"));
	}

	private void validateActorCanManageOrder(Order order, User actor, OrderStatus newStatus) {
		if (actor.getRole() == Role.ADMIN) {
			return;
		}

		if (actor.getRole() == Role.RESTAURANT_OWNER) {
			boolean ownsRestaurant = restaurantRepository
					.findByIdAndOwnerIdAndActiveTrue(order.getRestaurantId(), actor.getId()).isPresent();
			if (!ownsRestaurant) {
				throw new ForbiddenException("You can only manage orders for your own restaurant");
			}
			return;
		}

		if (actor.getRole() == Role.DELIVERY_AGENT) {
			if (order.getDeliveryAgentId() == null || !order.getDeliveryAgentId().equals(actor.getId())) {
				throw new ForbiddenException("You can only manage orders assigned to you");
			}
			return;
		}

		throw new ForbiddenException("You are not allowed to update order status");
	}

	private void validateTransition(Order order, User actor, OrderStatus newStatus) {
		OrderStatus currentStatus = order.getOrderStatus();

		if (currentStatus == newStatus) {
			throw new BadRequestException("Order is already in the requested status");
		}

		if (currentStatus == OrderStatus.CREATED) {
			throw new BadRequestException("Use the payment endpoint to move an order from CREATED to PAID");
		}

		if (currentStatus == OrderStatus.DELIVERED || currentStatus == OrderStatus.CANCELLED
				|| currentStatus == OrderStatus.FAILED) {
			throw new BadRequestException("This order can no longer be updated");
		}

		if (actor.getRole() == Role.RESTAURANT_OWNER) {
			if (currentStatus == OrderStatus.PAID
					&& (newStatus == OrderStatus.CONFIRMED || newStatus == OrderStatus.CANCELLED)) {
				return;
			}
			if (currentStatus == OrderStatus.CONFIRMED && newStatus == OrderStatus.PREPARING) {
				return;
			}
			throw new BadRequestException("Restaurant owners can only confirm, prepare, or cancel eligible orders");
		}

		if (actor.getRole() == Role.DELIVERY_AGENT) {
			if (order.getDeliveryAgentId() == null || !order.getDeliveryAgentId().equals(actor.getId())) {
				throw new ForbiddenException("Order is not assigned to you");
			}

			if (currentStatus == OrderStatus.PREPARING && newStatus == OrderStatus.OUT_FOR_DELIVERY) {
				return;
			}
			if (currentStatus == OrderStatus.OUT_FOR_DELIVERY && newStatus == OrderStatus.DELIVERED) {
				return;
			}
			throw new BadRequestException(
					"Delivery agents can only move assigned orders to OUT_FOR_DELIVERY or DELIVERED");
		}

		if (actor.getRole() == Role.ADMIN) {
			if (currentStatus == OrderStatus.PAID
					&& (newStatus == OrderStatus.CONFIRMED || newStatus == OrderStatus.CANCELLED)) {
				return;
			}
			if (currentStatus == OrderStatus.CONFIRMED
					&& (newStatus == OrderStatus.PREPARING || newStatus == OrderStatus.CANCELLED)) {
				return;
			}
			if (currentStatus == OrderStatus.PREPARING
					&& (newStatus == OrderStatus.OUT_FOR_DELIVERY || newStatus == OrderStatus.CANCELLED)) {
				return;
			}
			if (currentStatus == OrderStatus.OUT_FOR_DELIVERY && newStatus == OrderStatus.DELIVERED) {
				return;
			}
			throw new BadRequestException("Invalid status transition");
		}

		throw new ForbiddenException("You are not allowed to update order status");
	}

	private OrderResponse toResponse(Order order) {
		List<OrderItemResponse> items = order.getItems() == null ? List.of()
				: order.getItems().stream().map(item -> OrderItemResponse.builder().menuItemId(item.getMenuItemId())
						.name(item.getName()).price(item.getPrice()).quantity(item.getQuantity()).build()).toList();

		return OrderResponse.builder().orderId(order.getId()).userId(order.getUserId())
				.restaurantId(order.getRestaurantId()).deliveryAgentId(order.getDeliveryAgentId())
				.totalAmount(order.getTotalAmount()).status(order.getOrderStatus().name())
				.createdAt(order.getCreatedAt()).updatedAt(order.getUpdatedAt()).items(items).build();
	}
}