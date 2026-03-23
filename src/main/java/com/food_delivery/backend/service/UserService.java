package com.food_delivery.backend.service;

import com.food_delivery.backend.dto.*;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;

public interface UserService {

    UserResponse createUser(CreateUserRequest request);

    Page<UserResponse> getAllUsers(int page, int size);

    UserResponse getUserById(Long id);

    UserResponse getUserByEmail(String email);

    UserResponse updateUser(Long id, UpdateUserRequest request);

    void changePassword(Long id, ChangePasswordRequest request);

    void deleteUser(Long id);

    void blockUser(Long id);

    void unblockUser(Long id);

    Page<UserResponse> getDeliveryAgents(int page, int size);

    void activateAgent(Long id);

    void deactivateAgent(Long id);

    Map<String, Object> getAdminDashboard();
}
