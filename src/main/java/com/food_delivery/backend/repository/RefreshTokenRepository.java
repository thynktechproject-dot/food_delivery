package com.food_delivery.backend.repository;

import com.food_delivery.backend.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenAndRevokedFalse(String token);

    List<RefreshToken> findByUserIdAndRevokedFalse(Long userId);
}
