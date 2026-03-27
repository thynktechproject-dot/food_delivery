package com.food_delivery.backend.controller;

import com.food_delivery.backend.dto.ApiResponse;
import com.food_delivery.backend.dto.PaymentResponse;
import com.food_delivery.backend.dto.RazorpayOrderResponse;
import com.food_delivery.backend.dto.VerifyPaymentRequest;
import com.food_delivery.backend.service.PaymentService;
import com.food_delivery.backend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;
    private final UserService userService;

    // -------------------------------------------------------------------------
    // User Payment Endpoints
    // -------------------------------------------------------------------------

    @PostMapping("/api/user/payments/{orderId}/initiate")
    @PreAuthorize("hasRole('USER')")
    public ApiResponse<RazorpayOrderResponse> initiatePayment(
            @PathVariable Long orderId,
            Authentication authentication
    ) {
        log.info("POST /api/user/payments/{}/initiate called by user: {}", orderId, authentication.getName());

        Long userId = resolveUserId(authentication);
        log.debug("Initiating payment for orderId: {}, userId: {}", orderId, userId);

        return ApiResponse.<RazorpayOrderResponse>builder()
                .success(true)
                .message("Razorpay order created. Proceed to checkout.")
                .data(paymentService.createRazorpayOrder(orderId, userId))
                .build();
    }

    @PostMapping("/api/user/payments/verify")
    @PreAuthorize("hasRole('USER')")
    public ApiResponse<PaymentResponse> verifyPayment(
            @Valid @RequestBody VerifyPaymentRequest request,
            Authentication authentication
    ) {
        log.info("POST /api/user/payments/verify called by user: {}", authentication.getName());
        log.debug("Verifying payment for orderId: {}, razorpayOrderId: {}", 
                request.getOrderId(), request.getRazorpayOrderId());

        Long userId = resolveUserId(authentication);

        return ApiResponse.<PaymentResponse>builder()
                .success(true)
                .message("Payment verified successfully")
                .data(paymentService.verifyAndCapturePayment(request, userId))
                .build();
    }

    @GetMapping("/api/user/payments/order/{orderId}")
    @PreAuthorize("hasRole('USER')")
    public ApiResponse<PaymentResponse> getPayment(
            @PathVariable Long orderId,
            Authentication authentication
    ) {
        log.info("GET /api/user/payments/order/{} called by user: {}", orderId, authentication.getName());

        Long userId = resolveUserId(authentication);
        log.debug("Fetching payment for orderId: {}, userId: {}", orderId, userId);

        return ApiResponse.<PaymentResponse>builder()
                .success(true)
                .message("Payment fetched")
                .data(paymentService.getPaymentByOrderId(orderId, userId))
                .build();
    }

    // -------------------------------------------------------------------------
    // Razorpay Webhook — PUBLIC (no JWT, verified by Razorpay signature)
    // -------------------------------------------------------------------------

    @PostMapping("/api/payments/webhook")
    public ResponseEntity<Void> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("X-Razorpay-Signature") String razorpaySignature
    ) {
        log.info("POST /api/payments/webhook received");

        // Do NOT log full payload in production (can contain sensitive data)
        log.debug("Webhook received with signature: {}", razorpaySignature);

        paymentService.handleWebhook(payload, razorpaySignature);

        log.debug("Webhook processed successfully");

        return ResponseEntity.ok().build();
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private Long resolveUserId(Authentication authentication) {
        log.debug("Resolving userId for email: {}", authentication.getName());
        return userService.getUserByEmail(authentication.getName()).getId();
    }
}