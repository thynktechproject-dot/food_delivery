package com.food_delivery.backend.service.impl;

import com.food_delivery.backend.dto.DeliveryStatsResponse;
import com.food_delivery.backend.dto.LiveOrderResponse;
import com.food_delivery.backend.dto.OrderResponse;
import com.food_delivery.backend.dto.RecentOrderResponse;
import com.food_delivery.backend.dto.OrderItemResponse;
import com.food_delivery.backend.entity.*;
import com.food_delivery.backend.enums.RestaurantStatus;
import com.food_delivery.backend.exception.BadRequestException;
import com.food_delivery.backend.exception.ForbiddenException;
import com.food_delivery.backend.exception.ResourceNotFoundException;
import com.food_delivery.backend.mapper.OrderMapper;
import com.food_delivery.backend.repository.CartRepository;
import com.food_delivery.backend.repository.OrderRepository;
import com.food_delivery.backend.repository.PaymentRepository;
import com.food_delivery.backend.repository.RestaurantRepository;
import com.food_delivery.backend.repository.UserRepository;
import com.food_delivery.backend.service.OrderService;
import com.food_delivery.backend.util.PagingUtils;
import com.food_delivery.backend.notification.NotificationService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

	private final CartRepository cartRepository;
	private final OrderRepository orderRepository;
	private final UserRepository userRepository;
	private final RestaurantRepository restaurantRepository;
	private final NotificationService notificationService;
	private final PaymentRepository paymentRepository;

	private static final Set<OrderStatus> ACTIVE_DELIVERY_STATUSES = Set.of(OrderStatus.PREPARING,
			OrderStatus.OUT_FOR_DELIVERY);

	private static final Set<OrderStatus> HISTORY_STATUSES = Set.of(OrderStatus.DELIVERED);

	@Override
	@Transactional
	public OrderResponse placeOrder(Long userId) {
		log.info("Placing order for userId={}", userId);

		Cart cart = cartRepository.findByUserId(userId).orElseThrow(() -> {
			log.error("Cart not found for userId={}", userId);
			return new ResourceNotFoundException("Cart not found");
		});

		if (cart.getItems().isEmpty()) {
			log.warn("Cart is empty for userId={}", userId);
			throw new BadRequestException("Cart is empty");
		}

		restaurantRepository.findById(cart.getRestaurantId())
				.filter(restaurant -> restaurant.getStatus() == RestaurantStatus.APPROVED && restaurant.isActive())
				.orElseThrow(() -> {
					log.error("Restaurant not available for restaurantId={}", cart.getRestaurantId());
					return new BadRequestException("Orders can only be placed for available restaurants");
				});

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
		log.info("Order saved with orderId={}", savedOrder.getId());

		cart.getItems().clear();
		cart.setRestaurantId(null);
		cartRepository.save(cart);
		log.info("Cart cleared for userId={}", userId);

		return toResponse(savedOrder);
	}

	@Override
	public OrderResponse getOrderById(Long orderId) {
		log.info("Fetching order by orderId={}", orderId);
		return toResponse(getOrderOrThrow(orderId));
	}

	@Override
	public OrderResponse getOrderByUser(Long orderId, Long userId) {
		log.info("Fetching order by orderId={} and userId={}", orderId, userId);

		Order order = orderRepository.findByIdAndUserId(orderId, userId).orElseThrow(() -> {
			log.error("Order not found for orderId={} and userId={}", orderId, userId);
			return new ResourceNotFoundException("Order not found");
		});
		return toResponse(order);
	}

	@Override
	public OrderResponse getOrderByRestaurantOwner(Long orderId, Long ownerId) {
		log.info("Fetching order by restaurant owner ownerId={} orderId={}", ownerId, orderId);

		Long restaurantId = getOwnerRestaurantId(ownerId);
		Order order = orderRepository.findByIdAndRestaurantId(orderId, restaurantId).orElseThrow(() -> {
			log.error("Order not found for restaurantId={}", restaurantId);
			return new ResourceNotFoundException("Order not found");
		});
		return toResponse(order);
	}

	@Override
	public OrderResponse getOrderByDeliveryAgent(Long orderId, Long deliveryAgentId) {
		log.info("Fetching order by deliveryAgentId={} orderId={}", deliveryAgentId, orderId);

		Order order = orderRepository.findByIdAndDeliveryAgentId(orderId, deliveryAgentId).orElseThrow(() -> {
			log.error("Order not found for deliveryAgentId={}", deliveryAgentId);
			return new ResourceNotFoundException("Order not found");
		});
		return toResponse(order);
	}

	@Override
	@org.springframework.transaction.annotation.Transactional(readOnly = true)
	public Page<OrderResponse> getOrdersByUserId(Long userId, int page, int size) {
		log.info("Fetching orders for userId={} page={} size={}", userId, page, size);
		Pageable pageable = ordersPageable(page, size);
		return orderRepository.findByUserId(userId, pageable).map(this::toResponse);
	}

	@Override
	@org.springframework.transaction.annotation.Transactional(readOnly = true)
	public Page<OrderResponse> getOrdersByRestaurantOwner(Long ownerId, int page, int size) {
		log.info("Fetching orders for restaurant owner ownerId={} page={} size={}", ownerId, page, size);

		Long restaurantId = getOwnerRestaurantId(ownerId);
		return orderRepository.findByRestaurantId(restaurantId, ordersPageable(page, size)).map(this::toResponse);
	}

	@Override
	@org.springframework.transaction.annotation.Transactional(readOnly = true)
	public Page<OrderResponse> getOrdersByDeliveryAgentId(Long deliveryAgentId, int page, int size) {
		log.info("Fetching orders for deliveryAgentId={} page={} size={}", deliveryAgentId, page, size);

		return orderRepository.findByDeliveryAgentId(deliveryAgentId, ordersPageable(page, size)).map(this::toResponse);
	}

	@Override
	@org.springframework.transaction.annotation.Transactional(readOnly = true)
	public Page<OrderResponse> getActiveOrdersByDeliveryAgentId(Long deliveryAgentId, int page, int size) {
		log.info("Fetching active orders for deliveryAgentId={}", deliveryAgentId);

		return orderRepository.findByDeliveryAgentIdAndOrderStatusIn(deliveryAgentId, ACTIVE_DELIVERY_STATUSES,
				ordersPageable(page, size)).map(this::toResponse);
	}

	@Override
	@org.springframework.transaction.annotation.Transactional(readOnly = true)
	public Page<OrderResponse> getDeliveryHistoryByAgentId(Long deliveryAgentId, int page, int size) {
		log.info("Fetching delivery history for deliveryAgentId={}", deliveryAgentId);

		return orderRepository
				.findByDeliveryAgentIdAndOrderStatusIn(deliveryAgentId, HISTORY_STATUSES, ordersPageable(page, size))
				.map(this::toResponse);
	}

	@Override
	@org.springframework.transaction.annotation.Transactional(readOnly = true)
	public DeliveryStatsResponse getDeliveryStatsByAgentId(Long deliveryAgentId) {
		log.info("Fetching delivery stats for deliveryAgentId={}", deliveryAgentId);

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
		log.info("Fetching all orders page={} size={}", page, size);

		return orderRepository.findAllBy(ordersPageable(page, size)).map(this::toResponse);
	}

	@Override
	@Transactional
	public OrderResponse updateStatus(Long orderId, OrderStatus newStatus, String actorEmail) {
		log.info("Updating order status orderId={} newStatus={} actorEmail={}", orderId, newStatus, actorEmail);

		Order order = getOrderOrThrow(orderId);
		User actor = userRepository.findByEmailAndDeletedAtIsNull(actorEmail).orElseThrow(() -> {
			log.error("Actor not found email={}", actorEmail);
			return new ResourceNotFoundException("User not found");
		});

		validateActorCanManageOrder(order, actor, newStatus);
		validateTransition(order, actor, newStatus);

		order.setOrderStatus(newStatus);
		Order savedOrder = orderRepository.save(order);
		log.info("Order status updated orderId={} status={}", orderId, newStatus);

		if (newStatus == OrderStatus.DELIVERED) {
			User user = userRepository.findByIdAndDeletedAtIsNull(order.getUserId()).orElse(null);
			if (user != null) {
				log.info("Sending delivery notification to user={}", user.getEmail());
				notificationService.notifyUserOrderDelivered(user.getEmail(), "Order ID: " + order.getId());
			}
		}

		return toResponse(savedOrder);
	}

	@Override
	@Transactional
	public void cancelOrderByAdmin(Long orderId) {
		log.info("Cancelling order by admin orderId={}", orderId);

		Order order = getOrderOrThrow(orderId);
		if (order.getOrderStatus() == OrderStatus.DELIVERED) {
			log.warn("Attempt to cancel delivered order orderId={}", orderId);
			throw new BadRequestException("Delivered orders cannot be cancelled");
		}
		order.setOrderStatus(OrderStatus.CANCELLED);
		orderRepository.save(order);
	}

	@Override
	@Transactional
	public OrderResponse assignDeliveryAgent(Long orderId, Long deliveryAgentId) {
		log.info("Assigning delivery agent orderId={} deliveryAgentId={}", orderId, deliveryAgentId);

		Order order = getOrderOrThrow(orderId);
		User deliveryAgent = userRepository.findByIdAndDeletedAtIsNull(deliveryAgentId).orElseThrow(() -> {
			log.error("Delivery agent not found id={}", deliveryAgentId);
			return new ResourceNotFoundException("Delivery agent not found");
		});

		if (order.getOrderStatus() == OrderStatus.CREATED || order.getOrderStatus() == OrderStatus.PAID
				|| order.getOrderStatus() == OrderStatus.CANCELLED || order.getOrderStatus() == OrderStatus.DELIVERED
				|| order.getOrderStatus() == OrderStatus.FAILED) {
			log.warn("Invalid state for assigning delivery agent orderId={}", orderId);
			throw new BadRequestException("Delivery agent can only be assigned after the order reaches preparation");
		}

		if (deliveryAgent.getRole() != Role.DELIVERY_AGENT) {
			log.warn("User is not delivery agent id={}", deliveryAgentId);
			throw new BadRequestException("User is not a delivery agent");
		}

		if (!deliveryAgent.isActive()) {
			log.warn("Inactive delivery agent id={}", deliveryAgentId);
			throw new BadRequestException("Delivery agent must be active before assignment");
		}

		order.setDeliveryAgentId(deliveryAgentId);
		Order savedOrder = orderRepository.save(order);
		log.info("Delivery agent assigned orderId={} agentId={}", orderId, deliveryAgentId);

		User user = userRepository.findByIdAndDeletedAtIsNull(order.getUserId()).orElse(null);
		Restaurant restaurant = restaurantRepository.findById(order.getRestaurantId()).orElse(null);
		if (deliveryAgent != null && user != null && restaurant != null) {
			log.info("Sending notification to delivery agent id={}", deliveryAgentId);
			notificationService.notifyDeliveryAgentAssigned(deliveryAgent, order, user, restaurant);
		}

		return toResponse(savedOrder);
	}

	private Order getOrderOrThrow(Long orderId) {
		return orderRepository.findById(orderId).orElseThrow(() -> {
			log.error("Order not found orderId={}", orderId);
			return new ResourceNotFoundException("Order not found");
		});
	}

	private Long getOwnerRestaurantId(Long ownerId) {
		return restaurantRepository.findByOwnerIdAndActiveTrue(ownerId).map(Restaurant::getId).orElseThrow(() -> {
			log.error("Restaurant not found for ownerId={}", ownerId);
			return new ResourceNotFoundException("Restaurant not found");
		});
	}

	private Pageable ordersPageable(int page, int size) {
		return PagingUtils.pageRequest(page, size, Sort.by(Sort.Direction.DESC, "createdAt", "id"));
	}

	private void validateActorCanManageOrder(Order order, User actor, OrderStatus newStatus) {
		log.info("Validating actor permissions actorId={} role={} orderId={} newStatus={}", actor.getId(),
				actor.getRole(), order.getId(), newStatus);

		if (actor.getRole() == Role.ADMIN) {
			log.debug("Admin access granted for orderId={}", order.getId());
			return;
		}

		if (actor.getRole() == Role.RESTAURANT_OWNER) {
			boolean ownsRestaurant = restaurantRepository
					.findByIdAndOwnerIdAndActiveTrue(order.getRestaurantId(), actor.getId()).isPresent();
			if (!ownsRestaurant) {
				log.warn("Restaurant owner {} does not own restaurantId={}", actor.getId(), order.getRestaurantId());
				throw new ForbiddenException("You can only manage orders for your own restaurant");
			}
			log.debug("Restaurant owner authorized for orderId={}", order.getId());
			return;
		}

		if (actor.getRole() == Role.DELIVERY_AGENT) {
			if (order.getDeliveryAgentId() == null || !order.getDeliveryAgentId().equals(actor.getId())) {
				log.warn("Delivery agent {} not assigned to orderId={}", actor.getId(), order.getId());
				throw new ForbiddenException("You can only manage orders assigned to you");
			}
			log.debug("Delivery agent authorized for orderId={}", order.getId());
			return;
		}

		log.error("Unauthorized role {} tried to update orderId={}", actor.getRole(), order.getId());
		throw new ForbiddenException("You are not allowed to update order status");
	}

	private void validateTransition(Order order, User actor, OrderStatus newStatus) {
		OrderStatus currentStatus = order.getOrderStatus();

		log.info("Validating transition orderId={} currentStatus={} newStatus={} actorRole={}", order.getId(),
				currentStatus, newStatus, actor.getRole());

		if (currentStatus == newStatus) {
			log.warn("Same status transition attempted orderId={}", order.getId());
			throw new BadRequestException("Order is already in the requested status");
		}

		if (currentStatus == OrderStatus.CREATED) {
			log.warn("Invalid transition from CREATED orderId={}", order.getId());
			throw new BadRequestException("Use the payment endpoint to move an order from CREATED to PAID");
		}

		if (currentStatus == OrderStatus.DELIVERED || currentStatus == OrderStatus.CANCELLED
				|| currentStatus == OrderStatus.FAILED) {
			log.warn("Attempt to update finalized order orderId={}", order.getId());
			throw new BadRequestException("This order can no longer be updated");
		}

		if (actor.getRole() == Role.RESTAURANT_OWNER) {
			if (currentStatus == OrderStatus.PAID
					&& (newStatus == OrderStatus.CONFIRMED || newStatus == OrderStatus.CANCELLED)) {
				log.debug("Valid transition by restaurant owner orderId={}", order.getId());
				return;
			}
			if (currentStatus == OrderStatus.CONFIRMED && newStatus == OrderStatus.PREPARING) {
				log.debug("Valid preparing transition orderId={}", order.getId());
				return;
			}
			log.warn("Invalid transition by restaurant owner orderId={}", order.getId());
			throw new BadRequestException("Restaurant owners can only confirm, prepare, or cancel eligible orders");
		}

		if (actor.getRole() == Role.DELIVERY_AGENT) {
			if (order.getDeliveryAgentId() == null || !order.getDeliveryAgentId().equals(actor.getId())) {
				log.warn("Delivery agent {} not assigned to orderId={}", actor.getId(), order.getId());
				throw new ForbiddenException("Order is not assigned to you");
			}

			if (currentStatus == OrderStatus.PREPARING && newStatus == OrderStatus.OUT_FOR_DELIVERY) {
				log.debug("Valid transition to OUT_FOR_DELIVERY orderId={}", order.getId());
				return;
			}
			if (currentStatus == OrderStatus.OUT_FOR_DELIVERY && newStatus == OrderStatus.DELIVERED) {
				log.debug("Valid transition to DELIVERED orderId={}", order.getId());
				return;
			}
			log.warn("Invalid transition by delivery agent orderId={}", order.getId());
			throw new BadRequestException(
					"Delivery agents can only move assigned orders to OUT_FOR_DELIVERY or DELIVERED");
		}

		if (actor.getRole() == Role.ADMIN) {
			if (currentStatus == OrderStatus.PAID
					&& (newStatus == OrderStatus.CONFIRMED || newStatus == OrderStatus.CANCELLED)) {
				log.debug("Admin valid transition orderId={}", order.getId());
				return;
			}
			if (currentStatus == OrderStatus.CONFIRMED
					&& (newStatus == OrderStatus.PREPARING || newStatus == OrderStatus.CANCELLED)) {
				log.debug("Admin valid transition orderId={}", order.getId());
				return;
			}
			if (currentStatus == OrderStatus.PREPARING
					&& (newStatus == OrderStatus.OUT_FOR_DELIVERY || newStatus == OrderStatus.CANCELLED)) {
				log.debug("Admin valid transition orderId={}", order.getId());
				return;
			}
			if (currentStatus == OrderStatus.OUT_FOR_DELIVERY && newStatus == OrderStatus.DELIVERED) {
				log.debug("Admin delivered transition orderId={}", order.getId());
				return;
			}
			log.warn("Invalid admin transition orderId={}", order.getId());
			throw new BadRequestException("Invalid status transition");
		}

		log.error("Unauthorized role {} attempted transition orderId={}", actor.getRole(), order.getId());
		throw new ForbiddenException("You are not allowed to update order status");
	}

	private OrderResponse toResponse(Order order) {
		log.debug("Mapping order to response orderId={}", order.getId());

		List<OrderItemResponse> items = order.getItems() == null ? List.of()
				: order.getItems().stream().map(item -> OrderItemResponse.builder().menuItemId(item.getMenuItemId())
						.name(item.getName()).price(item.getPrice()).quantity(item.getQuantity()).build()).toList();

		return OrderResponse.builder().orderId(order.getId()).userId(order.getUserId())
				.restaurantId(order.getRestaurantId()).deliveryAgentId(order.getDeliveryAgentId())
				.totalAmount(order.getTotalAmount()).status(order.getOrderStatus().name())
				.createdAt(order.getCreatedAt()).updatedAt(order.getUpdatedAt()).items(items).build();
	}

	@Override
	public List<LiveOrderResponse> getLiveOrders(Long restaurantId) {

		log.info("Fetching live orders for restaurantId={}", restaurantId);

		if (restaurantId == null || restaurantId <= 0) {
			log.warn("Invalid restaurantId={}", restaurantId);
			throw new BadRequestException("Invalid restaurantId");
		}

		if (!restaurantRepository.existsById(restaurantId)) {
			log.error("Restaurant not found restaurantId={}", restaurantId);
			throw new ResourceNotFoundException("Restaurant not found");
		}

		List<OrderStatus> activeStatuses = List.of(OrderStatus.CREATED, OrderStatus.CONFIRMED, OrderStatus.PREPARING);

		return orderRepository.findByRestaurantIdAndOrderStatusIn(restaurantId, activeStatuses).stream().map(order -> {
			String customerName = getCustomerName(order.getUserId());
			return OrderMapper.toLiveOrderResponse(order, customerName);
		}).toList();
	}

	@Transactional
	@Override
	public List<RecentOrderResponse> getRecentOrders(Long restaurantId) {

		log.info("Fetching recent orders for restaurantId={}", restaurantId);

		if (restaurantId == null || restaurantId <= 0) {
			log.warn("Invalid restaurantId={}", restaurantId);
			throw new BadRequestException("Invalid restaurantId");
		}

		if (!restaurantRepository.existsById(restaurantId)) {
			log.error("Restaurant not found restaurantId={}", restaurantId);
			throw new ResourceNotFoundException("Restaurant not found");
		}

		List<OrderStatus> completedStatuses = List.of(OrderStatus.values());

		return orderRepository.findByRestaurantIdAndOrderStatusIn(restaurantId, completedStatuses,
				PageRequest.of(0, 10, Sort.by("createdAt").descending())).getContent().stream().map(order -> {

					Payment payment = paymentRepository.findByOrderId(order.getId()).orElse(null);

					String customerName = getCustomerName(order.getUserId());

					return OrderMapper.toRecentOrderResponse(order, payment, customerName);
				}).toList();
	}

	private String getCustomerName(Long userId) {

		log.debug("Fetching customer name for userId={}", userId);

		return userRepository.findById(userId).map(user -> user.getName()).orElseThrow(() -> {
			log.error("User not found with id={}", userId);
			return new ResourceNotFoundException("User not found with id: " + userId);
		});
	}
}