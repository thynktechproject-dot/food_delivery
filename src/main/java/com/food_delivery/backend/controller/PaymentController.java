package com.food_delivery.backend.controller;

import com.food_delivery.backend.dto.ApiResponse;
import com.food_delivery.backend.dto.PaymentResponse;
import com.food_delivery.backend.service.PaymentService;
import com.food_delivery.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user/payments")
@RequiredArgsConstructor
@PreAuthorize("hasRole('USER')")
public class PaymentController {

    private final PaymentService paymentService;
    private final UserService userService;

    @PostMapping("/{orderId}")
    public ApiResponse<PaymentResponse> pay(@PathVariable Long orderId, Authentication authentication) {
        Long userId = resolveCurrentUserId(authentication);

        PaymentResponse payment = paymentService.processPayment(orderId, userId);

        return ApiResponse.<PaymentResponse>builder()
                .success(true)
                .message("Payment processed")
                .data(payment)
                .build();
    }

    @GetMapping("/order/{orderId}")
    public ApiResponse<PaymentResponse> getPayment(@PathVariable Long orderId, Authentication authentication) {
        Long userId = resolveCurrentUserId(authentication);
        return ApiResponse.<PaymentResponse>builder()
                .success(true)
                .message("Payment fetched")
                .data(paymentService.getPaymentByOrderId(orderId, userId))
                .build();
    }

    private Long resolveCurrentUserId(Authentication authentication) {
        return userService.getUserByEmail(authentication.getName()).getId();
    }
}
