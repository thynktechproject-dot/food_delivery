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

@RestController
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final UserService userService;

    // -------------------------------------------------------------------------
    // User Payment Endpoints
    // -------------------------------------------------------------------------

    /**
     * Step 1 — Initiate payment.
     * Creates a Razorpay order and returns the details needed
     * for the frontend to open the Razorpay checkout popup.
     *
     * POST /api/user/payments/{orderId}/initiate
     *
     * Response includes: razorpayOrderId, amountInPaise, currency, keyId
     */
    @PostMapping("/api/user/payments/{orderId}/initiate")
    @PreAuthorize("hasRole('USER')")
    public ApiResponse<RazorpayOrderResponse> initiatePayment(
            @PathVariable Long orderId,
            Authentication authentication
    ) {
        Long userId = resolveUserId(authentication);
        return ApiResponse.<RazorpayOrderResponse>builder()
                .success(true)
                .message("Razorpay order created. Proceed to checkout.")
                .data(paymentService.createRazorpayOrder(orderId, userId))
                .build();
    }

    /**
     * Step 2 — Verify payment after checkout.
     * The frontend calls this with the three values Razorpay gives in its callback.
     * We verify the HMAC-SHA256 signature and mark the order as PAID.
     *
     * POST /api/user/payments/verify
     * Body: { orderId, razorpayOrderId, razorpayPaymentId, razorpaySignature }
     */
    @PostMapping("/api/user/payments/verify")
    @PreAuthorize("hasRole('USER')")
    public ApiResponse<PaymentResponse> verifyPayment(
            @Valid @RequestBody VerifyPaymentRequest request,
            Authentication authentication
    ) {
        Long userId = resolveUserId(authentication);
        return ApiResponse.<PaymentResponse>builder()
                .success(true)
                .message("Payment verified successfully")
                .data(paymentService.verifyAndCapturePayment(request, userId))
                .build();
    }

    /**
     * Get payment status for an order.
     *
     * GET /api/user/payments/order/{orderId}
     */
    @GetMapping("/api/user/payments/order/{orderId}")
    @PreAuthorize("hasRole('USER')")
    public ApiResponse<PaymentResponse> getPayment(
            @PathVariable Long orderId,
            Authentication authentication
    ) {
        Long userId = resolveUserId(authentication);
        return ApiResponse.<PaymentResponse>builder()
                .success(true)
                .message("Payment fetched")
                .data(paymentService.getPaymentByOrderId(orderId, userId))
                .build();
    }

    // -------------------------------------------------------------------------
    // Razorpay Webhook — PUBLIC (no JWT, verified by Razorpay signature)
    // -------------------------------------------------------------------------

    /**
     * Razorpay posts payment events here (payment.captured, payment.failed).
     * This endpoint is public — security comes from the HMAC signature check
     * inside the service, NOT from JWT auth.
     *
     * Configure this URL in your Razorpay Dashboard → Settings → Webhooks:
     *   https://your-domain.com/api/payments/webhook
     *
     * POST /api/payments/webhook
     * Header: X-Razorpay-Signature: <hmac>
     */
    @PostMapping("/api/payments/webhook")
    public ResponseEntity<Void> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("X-Razorpay-Signature") String razorpaySignature
    ) {
        paymentService.handleWebhook(payload, razorpaySignature);
        return ResponseEntity.ok().build();
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private Long resolveUserId(Authentication authentication) {
        return userService.getUserByEmail(authentication.getName()).getId();
    }
}