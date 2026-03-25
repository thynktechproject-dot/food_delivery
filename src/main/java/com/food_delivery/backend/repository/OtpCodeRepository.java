package com.food_delivery.backend.repository;

import com.food_delivery.backend.entity.OtpCode;
import com.food_delivery.backend.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OtpCodeRepository extends JpaRepository<OtpCode, Long> {

    void deleteByIdentifierAndRole(String identifier, Role role);
}
