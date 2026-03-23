package com.food_delivery.backend.config;

import com.food_delivery.backend.config.properties.BootstrapAdminProperties;
import com.food_delivery.backend.entity.Role;
import com.food_delivery.backend.entity.User;
import com.food_delivery.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminBootstrapInitializer implements ApplicationRunner {

    private final BootstrapAdminProperties bootstrapAdminProperties;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        if (!bootstrapAdminProperties.isEnabled()) {
            return;
        }

        if (!StringUtils.hasText(bootstrapAdminProperties.getName())
                || !StringUtils.hasText(bootstrapAdminProperties.getEmail())
                || !StringUtils.hasText(bootstrapAdminProperties.getPassword())) {
            throw new IllegalStateException("Bootstrap admin is enabled but credentials are incomplete");
        }

        if (userRepository.existsByEmailAndDeletedAtIsNull(bootstrapAdminProperties.getEmail())) {
            log.info("Bootstrap admin already exists for email {}", bootstrapAdminProperties.getEmail());
            return;
        }

        User admin = User.builder()
                .name(bootstrapAdminProperties.getName())
                .email(bootstrapAdminProperties.getEmail())
                .password(passwordEncoder.encode(bootstrapAdminProperties.getPassword()))
                .role(Role.ADMIN)
                .active(true)
                .build();

        userRepository.save(admin);
        log.info("Bootstrap admin created for email {}", bootstrapAdminProperties.getEmail());
    }
}
