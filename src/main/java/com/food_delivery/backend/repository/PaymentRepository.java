package com.food_delivery.backend.repository;

import com.food_delivery.backend.entity.Payment;
import com.food_delivery.backend.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

	Optional<Payment> findByOrderId(Long orderId);

	Optional<Payment> findTopByOrderIdOrderByIdDesc(Long orderId);

	boolean existsByOrderIdAndStatus(Long orderId, PaymentStatus status);

	/** Find payment by Razorpay's own order ID — used during webhook and verification. */
	Optional<Payment> findByRazorpayOrderId(String razorpayOrderId);
}