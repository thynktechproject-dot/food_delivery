package com.food_delivery.backend.service.impl;

import com.food_delivery.backend.dto.*;
import com.food_delivery.backend.entity.OrderStatus;
import com.food_delivery.backend.entity.User;
import com.food_delivery.backend.enums.RestaurantStatus;
import com.food_delivery.backend.entity.Role;
import com.food_delivery.backend.exception.*;
import com.food_delivery.backend.mapper.UserMapper;
import com.food_delivery.backend.repository.CartRepository;
import com.food_delivery.backend.repository.OrderRepository;
import com.food_delivery.backend.repository.RefreshTokenRepository;
import com.food_delivery.backend.repository.RestaurantRepository;
import com.food_delivery.backend.repository.UserRepository;
import com.food_delivery.backend.service.UserService;
import com.food_delivery.backend.util.PagingUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RestaurantRepository restaurantRepository;
    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserResponse createUser(CreateUserRequest request) {

        log.info("Creating user with email: {}", request.getEmail());

        if (userRepository.existsByEmailAndDeletedAtIsNull(request.getEmail())) {
            log.error("Email already exists: {}", request.getEmail());
            throw new BadRequestException("Email already exists");
        }

        User user = UserMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole() != null ? request.getRole() : Role.USER);
        User savedUser = userRepository.save(user);

        log.info("User created with ID: {}", savedUser.getId());

        return UserMapper.toResponse(savedUser);
    }

    @Override
    public Page<UserResponse> getAllUsers(int page, int size) {
        Pageable pageable = PagingUtils.pageRequest(page, size, Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        return userRepository.findByDeletedAtIsNull(pageable)
                .map(UserMapper::toResponse);
    }

    @Override
    public UserResponse getUserById(Long id) {

        log.info("Fetching user with ID: {}", id);

        User user = getActiveUserById(id)
                .orElseThrow(() -> {
                    log.error("User not found: {}", id);
                    return new ResourceNotFoundException("User not found");
                });

        return UserMapper.toResponse(user);
    }

    @Override
    public UserResponse getUserByEmail(String email) {

        User user = getActiveUserByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return UserMapper.toResponse(user);
    }

    @Override
    public UserResponse updateUser(Long id, UpdateUserRequest request) {

        log.info("Updating user ID: {}", id);

        User user = getActiveUserById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (userRepository.existsByEmailAndIdNotAndDeletedAtIsNull(request.getEmail(), id)) {
            throw new BadRequestException("Email already exists");
        }

        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setAddress(request.getAddress());

        User updatedUser = userRepository.save(user);

        log.info("User updated ID: {}", id);

        return UserMapper.toResponse(updatedUser);
    }

    @Override
    public void changePassword(Long id, ChangePasswordRequest request) {
        User user = getActiveUserById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadRequestException("Current password is incorrect");
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new BadRequestException("New password must be different from the current password");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setTokenVersion(user.getTokenVersion() + 1);
        userRepository.save(user);
        revokeRefreshTokens(user.getId());
    }

    @Override
    public void deleteUser(Long id) {
        log.info("Deleting user ID: {}", id);

        User user = getActiveUserById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getRole() == Role.ADMIN) {
            throw new BadRequestException("Admin user cannot be deleted");
        }

        if (user.getRole() == Role.RESTAURANT_OWNER && restaurantRepository.findByOwnerIdAndActiveTrue(user.getId()).isPresent()) {
            throw new BadRequestException("Restaurant owners must remove or transfer their restaurant before deleting the account");
        }

        if (user.getRole() == Role.DELIVERY_AGENT
                && orderRepository.countByDeliveryAgentIdAndOrderStatusIn(
                user.getId(),
                List.of(OrderStatus.CONFIRMED, OrderStatus.PREPARING, OrderStatus.OUT_FOR_DELIVERY)
        ) > 0) {
            throw new BadRequestException("Delivery agents with active assigned orders cannot be deleted");
        }

        cartRepository.findByUserId(user.getId()).ifPresent(cartRepository::delete);
        user.setActive(false);
        user.setDeletedAt(LocalDateTime.now());
        user.setTokenVersion(user.getTokenVersion() + 1);
        user.setName("Deleted User");
        user.setPhone(null);
        user.setAddress(null);
        user.setEmail(buildDeletedEmail(user.getEmail(), user.getId()));
        user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        userRepository.save(user);
        revokeRefreshTokens(user.getId());

        log.info("User deleted ID: {}", id);
    }

    @Override
    public void blockUser(Long id) {
        User user = getActiveUserById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getRole() == Role.ADMIN) {
            throw new BadRequestException("Admin user cannot be blocked");
        }

        user.setActive(false);
        user.setTokenVersion(user.getTokenVersion() + 1);
        userRepository.save(user);
        revokeRefreshTokens(user.getId());
    }

    @Override
    public void unblockUser(Long id) {
        User user = getActiveUserById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setActive(true);
        userRepository.save(user);
    }

    @Override
    public Page<UserResponse> getDeliveryAgents(int page, int size) {
        Pageable pageable = PagingUtils.pageRequest(page, size, Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        return userRepository.findByRoleAndDeletedAtIsNull(Role.DELIVERY_AGENT, pageable)
                .map(UserMapper::toResponse);
    }

    @Override
    public void activateAgent(Long id) {
        User agent = getActiveUserById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (agent.getRole() != Role.DELIVERY_AGENT) {
            throw new BadRequestException("User is not a delivery agent");
        }

        agent.setActive(true);
        userRepository.save(agent);
    }

    @Override
    public void deactivateAgent(Long id) {
        User agent = getActiveUserById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (agent.getRole() != Role.DELIVERY_AGENT) {
            throw new BadRequestException("User is not a delivery agent");
        }

        agent.setActive(false);
        userRepository.save(agent);
    }

    @Override
    public Map<String, Object> getAdminDashboard() {
        Map<String, Object> dashboard = new HashMap<>();
        dashboard.put("totalUsers", userRepository.countByDeletedAtIsNull());
        dashboard.put("activeUsers", userRepository.countByActiveTrueAndDeletedAtIsNull());
        dashboard.put("deliveryAgents", userRepository.countByRoleAndDeletedAtIsNull(Role.DELIVERY_AGENT));
        dashboard.put("totalRestaurants", restaurantRepository.countByActiveTrue());
        dashboard.put("approvedRestaurants", restaurantRepository.countByStatusAndActiveTrue(RestaurantStatus.APPROVED));
        dashboard.put("pendingRestaurants", restaurantRepository.countByStatusAndActiveTrue(RestaurantStatus.PENDING));
        dashboard.put("totalOrders", orderRepository.count());

        Map<String, Long> orderStatusCounts = new HashMap<>();
        for (OrderStatus status : OrderStatus.values()) {
            orderStatusCounts.put(status.name(), orderRepository.countByOrderStatus(status));
        }
        dashboard.put("orderStatusCounts", orderStatusCounts);

        return dashboard;
    }

    private String buildDeletedEmail(String currentEmail, Long userId) {
        String uniqueSuffix = userId + "-" + System.currentTimeMillis();
        return "deleted+" + uniqueSuffix + "@deleted.local";
    }

    private java.util.Optional<User> getActiveUserById(Long id) {
        return userRepository.findByIdAndDeletedAtIsNull(id);
    }

    private java.util.Optional<User> getActiveUserByEmail(String email) {
        return userRepository.findByEmailAndDeletedAtIsNull(email);
    }

    private void revokeRefreshTokens(Long userId) {
        refreshTokenRepository.findByUserIdAndRevokedFalse(userId)
                .forEach(token -> {
                    token.setRevoked(true);
                    token.setRevokedAt(LocalDateTime.now());
                    refreshTokenRepository.save(token);
                });
    }
}
