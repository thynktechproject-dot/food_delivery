package com.food_delivery.backend.repository;

import com.food_delivery.backend.entity.Role;
import com.food_delivery.backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailAndDeletedAtIsNull(String email);

    Optional<User> findByEmailAndRoleAndDeletedAtIsNull(String email, Role role);

    Optional<User> findByPhoneAndRoleAndDeletedAtIsNull(String phone, Role role);

    Optional<User> findByIdAndDeletedAtIsNull(Long id);

    boolean existsByEmailAndDeletedAtIsNull(String email);

    boolean existsByEmailAndIdNotAndDeletedAtIsNull(String email, Long id);

    Page<User> findByDeletedAtIsNull(Pageable pageable);

    List<User> findByRoleAndDeletedAtIsNull(Role role);

    Page<User> findByRoleAndDeletedAtIsNull(Role role, Pageable pageable);

    long countByDeletedAtIsNull();

    long countByRoleAndDeletedAtIsNull(Role role);

    long countByActiveTrueAndDeletedAtIsNull();

    java.util.List<User> findAllByDeletedAtIsNull();
}
