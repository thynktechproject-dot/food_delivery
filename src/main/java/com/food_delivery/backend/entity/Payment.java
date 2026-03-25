package com.food_delivery.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long orderId;

    @Column(nullable = false)
    private double amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    /**
     * Razorpay's order ID (e.g. order_AbCdEfGhIj1234).
     * Created by us when the user initiates payment.
     * Sent to the frontend so it can open the Razorpay checkout.
     */
    @Column(unique = true)
    private String razorpayOrderId;

    /**
     * Razorpay's payment ID (e.g. pay_AbCdEfGhIj1234).
     * Filled in after the user completes checkout and we verify the signature.
     * This is the final proof that money was received.
     */
    @Column(unique = true)
    private String razorpayPaymentId;

    /**
     * The HMAC-SHA256 signature we verified.
     * Stored for audit/dispute purposes.
     */
    private String razorpaySignature;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}