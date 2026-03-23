package com.food_delivery.backend.service.impl;

import com.food_delivery.backend.entity.Role;
import com.food_delivery.backend.entity.User;
import com.food_delivery.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {

        User user = userRepository.findByEmailAndDeletedAtIsNull(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        Role role = user.getRole() != null ? user.getRole() : Role.USER;

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPassword())
                .roles(role.name())
                .disabled(!user.isActive())
                .build();
    }
}
