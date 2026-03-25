package com.food_delivery.backend.service.impl;

import com.food_delivery.backend.config.properties.RazorpayProperties;
import com.food_delivery.backend.dto.PaymentResponse;
import com.food_delivery.backend.dto.RazorpayOrderResponse;
import com.food_delivery.backend.dto.VerifyPaymentRequest;
import com.food_delivery.backend.entity.*;
import com.food_delivery.backend.exception.BadRequestException;
import com.food_delivery.backend.exception.ConflictException;
import com.food_delivery.backend.exception.ForbiddenException;
import com.food_delivery.backend.exception.ResourceNotFoundException;
import com.food_delivery.backend.repository.OrderRepository;
import com.food_delivery.backend.repository.PaymentRepository;
import com.food_delivery.backend.service.PaymentService;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final RazorpayClient razorpayClient;
    private final RazorpayProperties razorpayProperties;

    // -------------------------------------------------------------------------
    // Step 1 — Create Razorpay Order
    // -------------------------------------------------------------------------

    /**
     * Creates a Razorpay order for a food delivery order.
     *
     * What happens:
     *  1. Validate the food delivery order belongs to this user and is in CREATED status.
     *  2. Call Razorpay API to create an order — gets back a razorpay_order_id.
     *  3. Save a Payment record with status PENDING and the razorpay_order_id.
     *  4. Return razorpay_order_id + key_id to the frontend so it can open checkout.
     */
    @Override
    @Transactional
    public RazorpayOrderResponse createRazorpayOrder(Long orderId, Long userId) {

        Order order = getOrderForUser(orderId, userId);

        if (order.getOrderStatus() == OrderStatus.PAID) {
            throw new ConflictException("This order has already been paid");
        }

        if (order.getOrderStatus() != OrderStatus.CREATED) {
            throw new BadRequestException("Only newly created orders can be paid");
        }

        // If a PENDING payment already exists (user reopened the page), reuse it
        return paymentRepository.findByOrderId(orderId)
                .filter(p -> p.getStatus() == PaymentStatus.PENDING)
                .map(existing -> buildRazorpayOrderResponse(order, existing))
                .orElseGet(() -> createNewRazorpayOrder(order));
    }

    private RazorpayOrderResponse createNewRazorpayOrder(Order order) {
        // Razorpay expects amount in paise (1 INR = 100 paise)
        long amountInPaise = Math.round(order.getTotalAmount() * 100);

        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", amountInPaise);
        orderRequest.put("currency", razorpayProperties.getCurrency());
        orderRequest.put("receipt", "order_" + order.getId());

        com.razorpay.Order razorpayOrder;
        try {
            razorpayOrder = razorpayClient.orders.create(orderRequest);
        } catch (RazorpayException e) {
            log.error("Razorpay order creation failed for order {}: {}", order.getId(), e.getMessage());
            throw new BadRequestException("Payment gateway error. Please try again.");
        }

        String razorpayOrderId = razorpayOrder.get("id");

        Payment payment = Payment.builder()
                .orderId(order.getId())
                .amount(order.getTotalAmount())
                .status(PaymentStatus.PENDING)
                .razorpayOrderId(razorpayOrderId)
                .build();

        paymentRepository.save(payment);

        log.info("Razorpay order created: {} for food order: {}", razorpayOrderId, order.getId());

        return buildRazorpayOrderResponse(order, payment);
    }

    private RazorpayOrderResponse buildRazorpayOrderResponse(Order order, Payment payment) {
        return RazorpayOrderResponse.builder()
                .orderId(order.getId())
                .razorpayOrderId(payment.getRazorpayOrderId())
                .amountInPaise(Math.round(order.getTotalAmount() * 100))
                .currency(razorpayProperties.getCurrency())
                .keyId(razorpayProperties.getKeyId())
                .build();
    }

    // -------------------------------------------------------------------------
    // Step 2 — Verify Payment Signature
    // -------------------------------------------------------------------------

    /**
     * After the user pays in the Razorpay checkout, the frontend receives:
     *   - razorpay_order_id
     *   - razorpay_payment_id
     *   - razorpay_signature
     *
     * We verify the signature using HMAC-SHA256:
     *   expected = HMAC_SHA256(razorpay_order_id + "|" + razorpay_payment_id, key_secret)
     *
     * If valid → mark payment SUCCESS, order PAID.
     * If invalid → mark payment FAILED, order FAILED.
     */
    @Override
    @Transactional
    public PaymentResponse verifyAndCapturePayment(VerifyPaymentRequest request, Long userId) {

        Order order = getOrderForUser(request.getOrderId(), userId);

        if (order.getOrderStatus() == OrderStatus.PAID) {
            throw new ConflictException("This order has already been paid");
        }

        Payment payment = paymentRepository.findByRazorpayOrderId(request.getRazorpayOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment record not found"));

        if (!payment.getOrderId().equals(request.getOrderId())) {
            throw new BadRequestException("Razorpay order ID does not match the food delivery order");
        }

        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            throw new ConflictException("Payment has already been verified");
        }

        boolean signatureValid = verifySignature(
                request.getRazorpayOrderId(),
                request.getRazorpayPaymentId(),
                request.getRazorpaySignature()
        );

        if (signatureValid) {
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setRazorpayPaymentId(request.getRazorpayPaymentId());
            payment.setRazorpaySignature(request.getRazorpaySignature());
            order.setOrderStatus(OrderStatus.PAID);
            log.info("Payment verified successfully for order {}, razorpay payment {}",
                    order.getId(), request.getRazorpayPaymentId());
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            order.setOrderStatus(OrderStatus.FAILED);
            log.warn("Invalid Razorpay signature for order {}", order.getId());
        }

        paymentRepository.save(payment);
        orderRepository.save(order);

        if (!signatureValid) {
            throw new BadRequestException("Payment verification failed. Invalid signature.");
        }

        return toResponse(payment);
    }

    // -------------------------------------------------------------------------
    // Webhook Handler
    // -------------------------------------------------------------------------

    /**
     * Razorpay sends webhook events when payment.captured or payment.failed occurs.
     * We verify the webhook signature using the webhook secret (different from key secret).
     * This is a safety net — the main flow is the signature verification above.
     */
    @Override
    @Transactional
    public void handleWebhook(String payload, String razorpaySignature) {

        // Verify the webhook came from Razorpay
        if (!verifyWebhookSignature(payload, razorpaySignature)) {
            log.warn("Invalid Razorpay webhook signature received");
            throw new BadRequestException("Invalid webhook signature");
        }

        JSONObject event = new JSONObject(payload);
        String eventType = event.getString("event");

        log.info("Razorpay webhook received: {}", eventType);

        JSONObject paymentEntity = event
                .getJSONObject("payload")
                .getJSONObject("payment")
                .getJSONObject("entity");

        String razorpayOrderId   = paymentEntity.getString("order_id");
        String razorpayPaymentId = paymentEntity.getString("id");

        paymentRepository.findByRazorpayOrderId(razorpayOrderId).ifPresent(payment -> {

            // Skip if already processed (idempotency)
            if (payment.getStatus() != PaymentStatus.PENDING) {
                log.info("Webhook skipped — payment {} already in status {}", razorpayOrderId, payment.getStatus());
                return;
            }

            Order order = orderRepository.findById(payment.getOrderId())
                    .orElse(null);
            if (order == null) return;

            switch (eventType) {
                case "payment.captured" -> {
                    payment.setStatus(PaymentStatus.SUCCESS);
                    payment.setRazorpayPaymentId(razorpayPaymentId);
                    order.setOrderStatus(OrderStatus.PAID);
                    log.info("Webhook: payment captured for order {}", payment.getOrderId());
                }
                case "payment.failed" -> {
                    payment.setStatus(PaymentStatus.FAILED);
                    order.setOrderStatus(OrderStatus.FAILED);
                    log.warn("Webhook: payment failed for order {}", payment.getOrderId());
                }
                default -> log.info("Webhook: unhandled event type {}", eventType);
            }

            paymentRepository.save(payment);
            orderRepository.save(order);
        });
    }

    // -------------------------------------------------------------------------
    // Fetch Payment
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByOrderId(Long orderId, Long userId) {
        getOrderForUser(orderId, userId); // ownership check
        Payment payment = paymentRepository.findTopByOrderIdOrderByIdDesc(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found for order"));
        return toResponse(payment);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private Order getOrderForUser(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        if (!order.getUserId().equals(userId)) {
            throw new ForbiddenException("You can only access your own orders");
        }
        return order;
    }

    /**
     * Verifies the payment signature:
     * expected = HMAC_SHA256(razorpayOrderId + "|" + razorpayPaymentId, keySecret)
     */
    private boolean verifySignature(String razorpayOrderId, String razorpayPaymentId, String signature) {
        String message = razorpayOrderId + "|" + razorpayPaymentId;
        return hmacSHA256Matches(message, razorpayProperties.getKeySecret(), signature);
    }

    /**
     * Verifies the webhook signature:
     * expected = HMAC_SHA256(rawPayload, webhookSecret)
     */
    private boolean verifyWebhookSignature(String payload, String signature) {
        return hmacSHA256Matches(payload, razorpayProperties.getWebhookSecret(), signature);
    }

    private boolean hmacSHA256Matches(String message, String secret, String expectedHex) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(
                    secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            byte[] hash = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            String computedHex = HexFormat.of().formatHex(hash);
            return computedHex.equals(expectedHex);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("HMAC-SHA256 verification error", e);
            return false;
        }
    }

    private PaymentResponse toResponse(Payment payment) {
        return PaymentResponse.builder()
                .orderId(payment.getOrderId())
                .amount(payment.getAmount())
                .status(payment.getStatus().name())
                .razorpayOrderId(payment.getRazorpayOrderId())
                .razorpayPaymentId(payment.getRazorpayPaymentId())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }
}