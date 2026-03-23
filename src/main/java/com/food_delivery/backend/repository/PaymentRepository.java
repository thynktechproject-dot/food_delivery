package com.food_delivery.backend.repository;

import com.food_delivery.backend.entity.Payment;
import com.food_delivery.backend.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

	Optional<Payment> findTopByOrderIdOrderByIdDesc(Long orderId);

	Optional<Payment> findByOrderId(Long orderId);

	boolean existsByOrderIdAndStatus(Long orderId, PaymentStatus status);
}
